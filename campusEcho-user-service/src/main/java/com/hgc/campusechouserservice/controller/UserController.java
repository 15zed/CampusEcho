package com.hgc.campusechouserservice.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.StringUtils;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;

import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechointerfaces.service.UserService;
import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;
import com.hgc.campusechomodel.entity.User;
import com.hgc.campusechouserservice.service.RedisService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 *
 */
@Controller
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;
    @Resource
    private RedisService redisService;
    @DubboReference
    private CountService countService;


    /**
     * 去往登录页
     *
     * @return 页面
     */
    @GetMapping("/forward")
    public String forward() {
        return "login";
    }


    /**
     * 登录
     * ✔
     * @param request 请求
     * @param user    用户
     * @return 页面
     */
    @PostMapping("/login")
    public ModelAndView login(HttpServletRequest request, User user) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        ModelAndView modelAndView = new ModelAndView();
        return userService.doLogin(modelAndView, request, user);
    }

    /**
     * 注册
     * ✔
     * @param user 请求传来的用户信息
     * @param file 用户的头像
     * @return
     */
    @PostMapping("/register")
    public ModelAndView register(User user, @RequestParam("file") MultipartFile file) {
        ModelAndView modelAndView = new ModelAndView();
        if (user == null || StringUtils.isAnyBlank(user.getUsername(),user.getPassword(),user.getContact())) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String uniqueId = userService.generateUniqueId(user);
        if(!redisService.setUniqueId(uniqueId)){
            modelAndView.setViewName("login");
            return modelAndView;
        }
        if (!file.isEmpty()) {
            String filename = userService.handFile(file);
            user.setHead(filename);
        }
        userService.add(user);
        modelAndView.setViewName("login");
        modelAndView.addObject("info", "注册成功");
        return modelAndView;
    }

    /**
     * 去某个用户主页 获取某个用户帖子和相关评论，分页展示
     * ✔
     * @param userId 目标主页用户的id
     * @return
     */
    @GetMapping("/{userId}")
    public ModelAndView toUserHome(@PathVariable("userId") Integer userId, HttpServletRequest request, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        ModelAndView modelAndView = new ModelAndView();
        userService.toUserHomeById(userId, modelAndView, request, page, size);
        return modelAndView;
    }

    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().removeAttribute("user");
        return "login";
    }

    /**
     * 添加关注
     * 需要校验是否达到关注上限
     *
     * @param userId  被关注人
     * @param session 获取关注人
     * @return 关注成功与否
     */
//    @PutMapping("/follow/{userId}")
//    public String follow(@PathVariable Integer userId, HttpSession session) {
//        if (userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        String jsonUser = (String) session.getAttribute("user");
//        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
//        User user = JSON.parseObject(jsonUser, User.class);
//        Set<String> follows = redisService.selectFollows(user.getUserId());
//        //todo: 考虑并发安全问题
//        if(follows == null){
//            Integer followCount = userService.selectFollowsCount(user.getUserId());
//            if(followCount > 2000) return "关注人数已达上限";
//        }else if(follows.size() > 2000){
//            return "关注人数已达上限";
//        }
//        //直接更新following表 INSERT
//        String result = userService.follow(user, userId, session);
//        //canalClient中异步更新follower表 INSERT
//        //canalClient异步更新计数服务（RPC远程调用） 关注数+1，粉丝数+1
//        //canalClient异步更新缓存：canal同步关注列表和粉丝列表缓存 INSERT
//        return result;
//    }


    /**
     * 添加关注
     * 需要校验是否达到关注上限 使用CAS操作进行重试循环
     * ✔
     * @param userId  被关注人
     * @param session 获取关注人
     * @return 关注成功与否
     */
    @PutMapping("/follow/{userId}")
    public String follow(@PathVariable Integer userId, HttpSession session) {
        if (userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);

        // 从Redis中获取当前关注列表
        Set<String> follows = redisService.selectFollows(user.getUserId());
        AtomicInteger followCount = new AtomicInteger(follows != null ? follows.size() : 0);

        // 使用CAS操作进行重试循环
        while (true) {
            int currentCount = followCount.get();
            if (currentCount >= 2000) {
                return "关注人数已达上限";
            }

            // 如果follows为空，从数据库中获取关注数量
            if (follows == null) {
                currentCount = userService.selectFollowsCount(user.getUserId());
                if (currentCount >= 2000) {
                    return "关注人数已达上限";
                }
                followCount.set(currentCount);  // 更新本地关注计数
            }

            // 尝试原子性地增加关注数量
            if (followCount.compareAndSet(currentCount, currentCount + 1)) {
                // 如果CAS操作成功，继续更新关注表，计数服务和缓存都会通过canal更新
                String result = userService.follow(user, userId, session);
                return result;
            }
        }
    }



    /**
     * 取消关注
     * ✔
     * @param userId  取消关注谁
     * @param session 获取取消人
     * @return
     */
    @PutMapping("/unfollow/{userId}")
    public String unfollow(@PathVariable Integer userId, HttpSession session) {
        if (userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        //直接更新following表 UPDATE
        String result = userService.cancel(user, userId, session);
        //canalClient异步更新follower表 UPDATE
        //canalClient异步更新计数服务
        //canalClient异步更新缓存：关注列表和粉丝列表缓存
        return result;
    }

    /**
     * 获取所有关注，关注上限2000可以分页，这里暂时不做分页
     * ✔
     * @param session
     * @return
     */
    @GetMapping("/follow/getFollows")
    public ModelAndView getFollows(HttpSession session) {
        ModelAndView modelAndView = new ModelAndView();
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        //先去redis缓存查
        Set<String>   followIds =  redisService.selectFollows(user.getUserId());
        if(followIds == null || followIds.isEmpty()){
            //再去数据库查
            List<Following> followingList = userService.selectFollows(user.getUserId());
            //存入redis缓存
            redisService.addFollows(user.getUserId(),followingList);
            //重新从redis获取followIds
            followIds = redisService.selectFollows(user.getUserId());
        }
        ArrayList<User> followList = new ArrayList<>(2000);
        for (String followId : followIds) {
            followList.add(userService.selectById(Integer.parseInt(followId)));
        }
        modelAndView.addObject("followList", followList);
        modelAndView.addObject("sessionUser", user);
        modelAndView.setViewName("myFollow");
        return modelAndView;
    }

    /**
     * 获取所有粉丝
     * 分页
     *
     * @param session
     * @return
     */
//    @GetMapping("/fans/getFans")
//    public ModelAndView getFans(HttpSession session) {
//        ModelAndView modelAndView = new ModelAndView();
//        String jsonUser = (String) session.getAttribute("user");
//        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
//        User user = JSON.parseObject(jsonUser, User.class);
//        //先去redis缓存查
//        Set<String> fansIds = redisService.selectFans(user.getUserId(),0,-1);
//        if(fansIds == null || fansIds.isEmpty()){
//            //再去数据库查
//            List<Follower> followerList = userService.selectFans(user.getUserId());
//            //存入redis缓存
//            redisService.addFans(user.getUserId(),followerList);
//            //存入fansIds
//            fansIds = redisService.selectFans(user.getUserId(),0,-1);
//        }
//        ArrayList<User> fansList = new ArrayList<>();
//        for (String fansId : fansIds) {
//            fansList.add(userService.selectById(Integer.parseInt(fansId)));
//        }
//        User newUser = userService.selectById(user.getUserId());
//        modelAndView.addObject("fansList", fansList);
//        modelAndView.addObject("userObj", newUser);
//        modelAndView.setViewName("myFans");
//        return modelAndView;
//    }

    /**
     * 获取所有粉丝
     * ✔
     * @param session
     * @param page 当前页
     * @param size 每一页大小
     * @return
     */
    @GetMapping("/fans/getFans")
    public ModelAndView getFans(HttpSession session,
                                @RequestParam(defaultValue = "1") int page,
                                @RequestParam(defaultValue = "10") int size) {
        ModelAndView modelAndView = new ModelAndView();
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        long start = (long) (page - 1) * size;
        long end = start + size - 1;
        ArrayList<User> fansList = new ArrayList<>();
        if(page <= 1000){
            Set<String> fansIds = redisService.selectFans(user.getUserId(),start,end);
            for (String fansId : fansIds) {
                fansList.add(userService.selectById(Integer.parseInt(fansId)));
            }
        }else{
            List<Follower> followerList = userService.selectFans(user.getUserId(),start,end);
            for (Follower follower : followerList) {
                fansList.add(userService.selectById(follower.getFromUserId()));
            }
        }
        User newUser = userService.selectById(user.getUserId());
        modelAndView.addObject("fansList", fansList);
        modelAndView.addObject("userObj", newUser);
        modelAndView.setViewName("myFans");
        return modelAndView;
    }




    /**
     * 查询用户的关注关系
     * ✔
     * @param userId  目标用户
     * @param session 当前用户
     * @return 关注关系 1：当前用户关注了目标用户  2：互关 3：当前用户未关注目标用户
     */
    @GetMapping("/getRelations")
    @ResponseBody
    public Integer selectRelations(@PathVariable Integer userId, HttpSession session) {
        Integer result = null;
        if (userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        // 先去缓存查看当前用户是否关注目标用户
        Long  rank = redisService.selectIfFollow(user.getUserId(),userId);
        if(rank == null){
            // 缓存没有，再去数据库查看
            Following  res = userService.selectIfFollow(user.getUserId(),userId);
            if(res == null){
                result = 3;
            }else {
                // 数据库有，证明当前用户已关注目标用户，存入缓存(更新当前用户的关注列表和目标用户的粉丝列表)
                redisService.addFollows(user.getUserId(), Arrays.asList(res));
                Follower follower = new Follower(res.getId(), res.getFromUserId(), res.getToUserId(), res.getType(), res.getUpdateTime());
                redisService.addFans(userId, Arrays.asList(follower));
                // 再从缓存中查询当前用户的粉丝列表是否包含目标用户（目标用户是否也关注了当前用户）
                Long rank1 = redisService.selectIfFollower(user.getUserId(),userId);
                if(rank1 == null){
                    // 缓存没有，再去数据库查看
                    Follower res2 = userService.selectIfFollower(user.getUserId(),userId);
                    if(res2 == null){
                        result = 1;
                    }else {
                        // 数据库有，存入缓存(更新当前用户粉丝列表)
                        redisService.addFans(user.getUserId(), Arrays.asList(res2));
                        result = 2;
                    }
                }
            }
        }else {
            // 再从缓存中查询目标用户是否也关注了当前用户
            Long rank2 = redisService.selectIfFollower(user.getUserId(),userId);
            if(rank2 == null){
                // 缓存没有，再去数据库查看
                Follower res2 = userService.selectIfFollower(user.getUserId(),userId);
                if(res2 == null){
                    result = 1;
                }else {
                    // 数据库有，存入缓存
                    redisService.addFans(user.getUserId(), Arrays.asList(res2));
                    result = 2;
                }
            }
        }
        return result;
    }

}

package com.hgc.school.controller;

import com.alibaba.fastjson.JSON;

import com.github.rholder.retry.*;
import com.hgc.school.commons.ErrorCode;
import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.exception.BusinessException;
import com.hgc.school.service.*;
import com.hgc.school.task.HotPostsTask;
import com.hgc.school.task.RecommendPostsTask;
import com.hgc.school.utils.ESIndexUtil;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Flag;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@RestController
@RequestMapping("/api")
public class APIController {
    @Autowired
    private FlagService flagService;
    @Autowired
    private RecommendPostsTask recommendPostsTask;
    @Autowired
    private ESService esService;
    @Autowired
    private RedisService redisService;
    @Autowired
    private InfoService infoService;

    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private HotPostsTask hotPostsTask;


    @RequestMapping("/welcome")
    public ModelAndView toWelcome() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("welcome");
        return modelAndView;
    }

    /**
     * 获取所有数据 帖子和评论
     *
     * @return
     */
    @RequestMapping("/getdata")
    public String getData() {
        List<InfoWithCommentsDTO> result = infoService.getAllInfoAndComments();
        return JSON.toJSONString(result);
    }

    /**
     * 添加关注
     * 使用事务
     *
     * @param userId  被关注人
     * @param session 获取关注人
     * @return
     */
    @PutMapping("/follow/{userId}")
    public String follow(@PathVariable Integer userId, HttpSession session) {
        if (userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        return userService.follow(user, userId, session);
    }

    /**
     * 取消关注
     * 使用事务
     *
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
        return userService.cancel(user, userId, session);
    }


    /**
     * 获取所有关注
     * 使用了缓存，提高了效率
     *
     * @param session
     * @return
     */
    @GetMapping("/follow/getFollows")
    public ModelAndView getFollows(HttpSession session) {
        ModelAndView modelAndView = new ModelAndView();
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        List<User> followList = userService.selectFollows(user.getUserId());
        modelAndView.addObject("followList", followList);
        modelAndView.addObject("sessionUser", user);
        modelAndView.setViewName("myFollow");
        return modelAndView;
    }

    /**
     * 获取所有粉丝
     * 使用了缓存，提高了效率
     *
     * @param session
     * @return
     */
    @GetMapping("/fans/getFans")
    public ModelAndView getFans(HttpSession session) {
        ModelAndView modelAndView = new ModelAndView();
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        List<User> fansList = userService.selectFans(user.getUserId());
        User newUser = userService.selectById(user.getUserId());
        modelAndView.addObject("fansList", fansList);
        modelAndView.addObject("userObj", newUser);
        modelAndView.setViewName("myFans");
        return modelAndView;
    }


    /**
     * 上传图片
     *
     * @param file
     * @return 文件名
     */
    @PostMapping("/uploadImage")
    public String upload(@RequestParam("image") MultipartFile file) {
        if (file.isEmpty()) return null;
        return userService.handFile(file);
    }

    /**
     * 发帖子
     *
     * @param info
     * @return
     */
    @PostMapping("/post")
    public String addInfo(@RequestBody Info info) throws ExecutionException, InterruptedException {
        if (info == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String uniqueId = infoService.generateUniqueId(info);
        if (!redisService.setUniqueId(uniqueId)) {
            return null;
        }
        //这里设置了会把生成的主键id赋值给info对象的id字段 返回info的所有信息 不然前端的有些操作会拿不到帖子的id
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(result -> result)//如果结果为true，重试，这里没有设置暂停策略，所以会一直重试结果为false
                .withWaitStrategy(WaitStrategies.fixedWait(5L, TimeUnit.SECONDS))
                .build();
        Flag flag = new Flag();//这个对象是为了在lambda表达式中绕过对变量必须为final的要求
        Callable<Boolean> retryTask = () -> {
            flag.mysqlFlag = infoService.add(info).get();
            flag.esFlag = esService.add(ESIndexUtil.INFO_INDEX, info).get();
            //// 只要其中一个成功（非0），就返回false结束重试
            return flag.mysqlFlag == 0 && flag.esFlag == 0;
        };
        try {
            // //如果MySQL和ES都失败，那么重新操作，直到其中一个成功，不需要全部都成功，因为采用的是弱一致方案，有定时任务补偿
            retryer.call(retryTask);
            flagService.addFlag(new Flag(info.getId(), flag.mysqlFlag, 1, flag.esFlag, 0, 1));
        } catch (RetryException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return JSON.toJSONString(info);
    }

    /**
     * 发评论
     *
     * @param comment
     * @return
     */
    @PostMapping("/comment")
    public String addComment(@RequestBody CommentInfo comment) throws ExecutionException, InterruptedException {
        if (comment == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String uniqueId = commentService.generateUniqueId(comment);
        if (!redisService.setUniqueId(uniqueId)) {
            return null;
        }
        //更新ES
        Integer comments = infoService.selectById(comment.getPubId()).getComments();
        Map<String, Object> doc = new HashMap<>();
        doc.put("comments", comments);
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(result -> result)//如果结果为true，重试，这里没有设置暂停策略，所以会一直重试结果为false
                .withWaitStrategy(WaitStrategies.fixedWait(5L, TimeUnit.SECONDS))
                .build();
        Flag flag = new Flag();//这个对象是为了在lambda表达式中绕过对变量必须为final的要求
        Callable<Boolean> retryTask = () -> {
            Integer addFlag1 = commentService.add(comment).get();
            Integer addFlag2 = infoService.updateComments(comment.getPubId()).get();
            Integer esFlag1 = esService.add(ESIndexUtil.COMMENT_INDEX, comment).get();
            Integer esFlag2 = esService.update(ESIndexUtil.INFO_INDEX, String.valueOf(comment.getPubId()), doc).get();
            flag.mysqlFlag = (addFlag1 & addFlag2);
            flag.esFlag = (esFlag1 & esFlag2);
            //// 只要其中一个成功（非0），就返回false结束重试
            return flag.mysqlFlag == 0 && flag.esFlag == 0;
        };
        try {
            // //如果MySQL和ES都失败，那么重新操作，直到其中一个成功，不需要全部都成功，因为采用的是弱一致方案，有定时任务补偿
            retryer.call(retryTask);
            flagService.addFlag(new Flag(comment.getCommentId(), flag.mysqlFlag, 1, flag.esFlag, 0, 2));
        } catch (RetryException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return JSON.toJSONString(comment);
    }


    /**
     * 点赞
     *
     * @param id      帖子的id
     * @param request
     * @return
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> addLikes(@PathVariable("id") Integer id, HttpServletRequest request) throws ExecutionException, InterruptedException {
        if (id == null || id.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String jsonUser = (String) request.getSession().getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        // 更新mysql:用户表添加点赞列表，帖子表添加点赞数

        //删除redis
        Integer redisFlag = redisService.delete(user.getUserId()).get();
        //更新ES
        // 返回更新后的帖子数据
        Info info = infoService.selectById(id);
        Integer likes = info.getLikes() + 1;
        // 构建部分更新的文档
        Map<String, Object> doc = new HashMap<>();
        doc.put("likes", likes);
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(result -> result)//如果结果为true，重试，这里没有设置暂停策略，所以会一直重试结果为false
                .withWaitStrategy(WaitStrategies.fixedWait(5L, TimeUnit.SECONDS))
                .build();
        Flag flag = new Flag();//这个对象是为了在lambda表达式中绕过对变量必须为final的要求
        Callable<Boolean> retryTask = () -> {
            Integer flag1 = userService.addPostToUserLikes(user.getUserId(), id).get();
            Integer flag2 = infoService.updateById(id).get();
            flag.mysqlFlag = (flag1 & flag2);
            flag.esFlag = esService.update(ESIndexUtil.INFO_INDEX, String.valueOf(id), doc).get();
            // 只要其中一个成功（非0），就返回false结束重试
            return flag.mysqlFlag == 0 && flag.esFlag == 0;
        };
        try {
            //如果MySQL和ES都失败，那么重新操作，直到其中一个成功，不需要全部都成功，因为采用的是弱一致方案，有定时任务补偿
            retryer.call(retryTask);
            flagService.addFlag(new Flag(id, flag.mysqlFlag, redisFlag, flag.esFlag, user.getUserId(), 3));
        } catch (RetryException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResponseEntity
                .ok()
                .body(infoService.selectById(id));
    }

    /**
     * 取消点赞
     *
     * @param id      帖子的id
     * @param request
     * @return
     */
    @PostMapping("/{id}/dislike")
    public ResponseEntity<?> cancelLikes(@PathVariable("id") Integer id, HttpServletRequest request) throws ExecutionException, InterruptedException {
        if (id == null || id.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String jsonUser = (String) request.getSession().getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        // 更新mysql:用户表取消点赞列表，帖子表减少点赞数

        //删除redis
        Integer redisFlag = redisService.delete(user.getUserId()).get();
        //更新ES
        // 返回更新后的帖子数据
        Info info = infoService.selectById(id);
        Integer likes = info.getLikes() - 1;
        // 构建部分更新的文档
        Map<String, Object> doc = new HashMap<>();
        doc.put("likes", likes);
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(result -> result)//如果结果为true，重试，这里没有设置暂停策略，所以会一直重试结果为false
                .withWaitStrategy(WaitStrategies.fixedWait(5L, TimeUnit.SECONDS))
                .build();
        Flag flag = new Flag();//这个对象是为了在lambda表达式中绕过对变量必须为final的要求
        Callable<Boolean> retryTask = () -> {
            Integer flag1 = userService.cancelPostToUserLikes(user.getUserId(), id).get();
            Integer flag2 = infoService.decreaseUpdateById(id).get();
            flag.mysqlFlag = (flag1 & flag2);
            flag.esFlag = esService.update(ESIndexUtil.INFO_INDEX, String.valueOf(id), doc).get();
            // 只要其中一个成功（非0），就返回false结束重试
            return flag.mysqlFlag == 0 && flag.esFlag == 0;
        };
        try {
            //如果MySQL和ES都失败，那么重新操作，直到其中一个成功，不需要全部都成功，因为采用的是弱一致方案，有定时任务补偿
            retryer.call(retryTask);
            flagService.addFlag(new Flag(id, flag.mysqlFlag, redisFlag, flag.esFlag, user.getUserId(), 3));
        } catch (RetryException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return ResponseEntity
                .ok()
                .body(infoService.selectById(id));
    }

    /**
     * 删除帖子和评论
     *
     * @param id 帖子id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public String deleteInfoWithComments(@PathVariable("id") Integer id, HttpServletRequest request) throws ExecutionException, InterruptedException {
        if (id == null || id.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<Integer> commentIdList = commentService.selectIdsByInfoId(id);
        Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(result -> result)//如果结果为true，重试，这里没有设置暂停策略，所以会一直重试结果为false
                .withWaitStrategy(WaitStrategies.fixedWait(5L, TimeUnit.SECONDS))
                .build();
        Flag flag = new Flag();//这个对象是为了在lambda表达式中绕过对变量必须为final的要求
        Callable<Boolean> retryTask = () -> {
            flag.mysqlFlag = infoService.deleteInfoWithComments(id).get();
            flag.esFlag = esService.delete(String.valueOf(id), commentIdList).get();
            // 只要其中一个成功（非0），就返回false结束重试
            return flag.mysqlFlag == 0 && flag.esFlag == 0;
        };
        User user;
        try {
            //如果MySQL和ES都失败，那么重新操作，直到其中一个成功，不需要全部都成功，因为采用的是弱一致方案，有定时任务补偿
            retryer.call(retryTask);
            flagService.addFlag(new Flag(id, flag.mysqlFlag, 1, flag.esFlag, 0, 4));
            String jsonUser = (String) request.getSession().getAttribute("user");
            user = JSON.parseObject(jsonUser, User.class);
        } catch (RetryException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR);
        }
        return "redirect:/user/" + user.getUserId();
    }

    /**
     * 获取热点帖子数据
     *
     * @return
     */
    @RequestMapping("/hotPosts")
    public String getHotPosts() {
        //调用定时任务返回热点帖子数据，转成json给前端
        List<InfoWithCommentsDTO> hotPosts = hotPostsTask.getHotPosts();
        return JSON.toJSONString(hotPosts);
    }

    /**
     * ES查询帖子
     *
     * @param keyword
     * @return
     */
    @GetMapping("/search/{keyword}")
    public String searchPost(@PathVariable String keyword) {
        if (keyword == null || keyword.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<InfoWithCommentsDTO> dtoList = esService.search(keyword);
        return JSON.toJSONString(dtoList);
    }

    /**
     * 推荐帖子
     *
     * @return
     */
    @GetMapping("/recommendPosts")
    public String getRecommendPosts(HttpSession session) {
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        //调用定时任务返回推荐帖子数据，转成json给前端
        List<InfoWithCommentsDTO> recommendPosts = recommendPostsTask.getRecommendPosts(user.getUserId());
        return JSON.toJSONString(recommendPosts);
    }
}

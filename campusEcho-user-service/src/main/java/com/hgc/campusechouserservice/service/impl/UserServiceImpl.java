package com.hgc.campusechouserservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.nacos.common.utils.StringUtils;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.constant.FileConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechointerfaces.service.UserService;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechomodel.entity.User;
import com.hgc.campusechouserservice.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.Session;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Future;

/**
 *
 */
@Service
@Slf4j
@DubboService
public class UserServiceImpl implements UserService {
    @Resource
    @Lazy
    private UserService userService;
    @DubboReference
    private InfoService infoService;
    @DubboReference
    private CommentService commentService;
    @DubboReference
    private CountService countService;

    @Resource
    private UserMapper userMapper;
    @Resource
    private JedisPool jedisPool;

    @Override
    public String generateUniqueId(User user) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // 把用户信息转换成字符串
        String userString = user.toString();
        try {
            // 创建 MessageDigest 对象，指定使用 SHA-256 算法进行哈希计算
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 对用户信息进行哈希计算
            byte[] hashBytes = digest.digest(userString.getBytes());
            // 使用 Base64 编码将哈希结果转换成字符串
            String hashString = Base64.getEncoder().encodeToString(hashBytes);
            // 将用户name和联系方式与哈希字符串拼接起来作为唯一标识符
            return user.getUsername() + "-" + user.getContact() + "-" + hashString;
        }
        catch (NoSuchAlgorithmException e) {
            // 哈希算法不支持时抛出异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法生成唯一Id");
        }
    }

    @Override
    public User selectUser(String username) {
        if (username == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.selectUser(username);
    }

    @Override
    public int add(User user) {
        if (user == null || StringUtils.isAnyBlank(user.getUsername(), user.getPassword(), user.getContact()))
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.addUser(user);
    }

    @Override
    public User selectById(Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.selectById(userId);
    }



    /**
     * 关注
     *
     * @param userId1 关注人
     * @param userId2 被关注人
     * @return
     */
    @Override
    public boolean addFollow(Integer userId1, Integer userId2) {
        if (userId1 == null || userId2 == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // 判断是否有数据记录
        Following following = userMapper.selectIfExists(userId1, userId2);
        if(following == null){
            userMapper.addFollow(userId1, userId2);
            return true;
        }else {
            int type = 1;
            userMapper.updateFollow(userId1, userId2, type);
            return false;
        }
    }

    @Override
    public List<Following> selectFollows(Integer userId) {
        if (userId == null || userId == 0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.selectFollowingIds(userId);
    }



    /**
     * 取消关注
     *
     * @param user    取消人
     * @param userId  取消关注谁
     * @param session
     * @return
     */
    @Override
    public String cancel(User user, Integer userId, HttpSession session) {
        if (user == null || userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        if (userService.unFollow(user.getUserId(), userId)) {
            // 更新session中的用户信息
            session.setAttribute("user", JSON.toJSONString(user));
            return "取消成功";
        }
        else
            return "取消失败";
    }


    /**
     * 取消关注
     *
     * @param userId1 操作人
     * @param userId2 取消谁
     * @return
     */
    @Override
    public boolean unFollow(Integer userId1, Integer userId2) {
        if (userId1 == null || userId2 == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // 判断是否已经关注,如果已经关注，则取消关注
        Following following = userMapper.selectFollowing(userId1, userId2);
        if(following != null){
            int type = 2;
            userMapper.updateFollow(userId1, userId2,type);
            return true;
        }else {
            return false;
        }
    }



    @Override
    public List<Follower> selectFans(Integer userId,long offset,long limit) {
        if (userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.selectFollowerIds(userId,offset,limit);
    }




    /**
     * 登录
     *
     * @param modelAndView
     * @param request      请求
     * @param user         请求传来的用户信息
     * @return
     */
    @Override
    public ModelAndView doLogin(ModelAndView modelAndView, HttpServletRequest request, User user) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Jedis jedis = jedisPool.getResource();
        //先去缓存查
        String userId = jedis.get("user:" + user.getUsername());
        String jsonUser = jedis.get("user:" + userId);
        //缓存没有
        if (jsonUser == null || jsonUser.isEmpty()) {
            User selectUser = this.selectUser(user.getUsername());
            if (selectUser == null) {
                modelAndView.setViewName("register");
                return modelAndView;
            }
            choose(selectUser, user, request, jedis, modelAndView);//逻辑处理，决定去哪个页面
            return modelAndView;
        }
        //缓存有
        User selectUser = JSON.parseObject(jsonUser, User.class);
        choose(selectUser, user, request, jedis, modelAndView);
        return modelAndView;
    }

    /**
     * 登录处理
     *
     * @param selectUser   数据库或者缓存中的用户信息
     * @param user         请求传入的用户信息
     * @param request      请求
     * @param jedis        连接对象
     * @param modelAndView
     */
    @Override
    public void choose(User selectUser, User user, HttpServletRequest request, Jedis jedis, ModelAndView modelAndView) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        //密码不等 返回重新登录
        if (!selectUser.getPassword().equals(user.getPassword())) {
            modelAndView.addObject("info", "密码错误");
            modelAndView.setViewName("login");
            return;
        }
        //密码相等 判断状态 账号被禁用 直接返回
        if (selectUser.getStatus() == 0) {
            modelAndView.addObject("info", "账号被禁用");
            modelAndView.setViewName("login");
            return;
        }
        //上面都没有返回 说明登录成功
        request.getSession().setAttribute("user", JSON.toJSONString(selectUser));//信息放session里
        jedis.setex("user:" + selectUser.getUserId(), 86400, JSON.toJSONString(selectUser));//续期
        userMapper.updateLoginTime(selectUser.getUserId());//更新登录时间
        modelAndView.setViewName("welcome");//跳转主页
        jedis.close();
    }

    /**
     * 处理用户传的文件 包括头像和帖子中的照片
     *
     * @param file 用户上传的文件
     * @return 文件名
     */
    @Override
    public String handFile(MultipartFile file) {
        if (file.isEmpty()) return null;
        String originalFilename = file.getOriginalFilename();
        //截取原始文件名后缀 .jpg  .png  .....
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //拼接成最后的文件名
        String filename = UUID.randomUUID() + suffix;
        try {
            file.transferTo(new File(FileConstant.FILE_PREFIX + filename));
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return filename;
    }

    /**
     * 去用户主页 获取某个用户帖子和相关评论，分页展示
     *
     * @param userId       主页用户的id
     * @param modelAndView
     * @param request      请求
     */
    @Override
    public void toUserHomeById(Integer userId, ModelAndView modelAndView, HttpServletRequest request,int page,int size) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        int start =  (page - 1) * size;
        int end = start + size - 1;
        //先去缓存查
        Jedis jedis = jedisPool.getResource();
        String jUser = jedis.get("user:" + userId);
        if (jUser == null || jUser.isEmpty()) {
            User user = this.selectById(userId);
            if (user == null) return;
            jedis.setex("user:" + user.getUserId(), 86400, JSON.toJSONString(user));
            jUser = jedis.get("user:" + userId);
        }
        User user = JSON.parseObject(jUser, User.class);
        List<InfoWithCommentsDTO> dtoList = new ArrayList<>();
        //远程调用计数服务，查询关注数和粉丝数
        Long fans = countService.getFans(userId);
        Long follows = countService.getFollows(userId);
        //远程调用帖子服务 查询帖子，分页
        List<Info> infoList = infoService.selectInfoByPage(userId,start,end);
        //远程调用评论服务 查询评论，分页
        for (Info info : infoList) {
            List<CommentInfo> comments = commentService.selectByInfoId(info.getId(), 1l, 10l);
            dtoList.add(new InfoWithCommentsDTO(info, comments));
        }
        String jsonUser = (String) request.getSession().getAttribute("user");
        User sessionUser = JSON.parseObject(jsonUser, User.class);
//        sessionUser = change(sessionUser);
        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.addObject("user", user);
        modelAndView.addObject("follows", follows);
        modelAndView.addObject("fans", fans);
        modelAndView.addObject("dtoList", dtoList);
        modelAndView.setViewName("userhome");
        jedis.close();
    }

//    @Override
//    public User change(User user) {
//        if(user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
//        String follows = user.getFollows();
//        String fans = user.getFans();
//        follows = follows.replace("0,","");
//        follows = follows.replace("0","");
//        fans = fans.replace("0,","");
//        fans = fans.replace("0","");
//        user.setFollows(follows);
//        user.setFans(fans);
//        return user;
//    }

    /**
     * 处理关注逻辑
     *
     * @param user    关注人
     * @param userId  被关注人id
     * @param session
     * @return
     */
    @Override
    public String follow(User user, Integer userId, HttpSession session) {
        if (user == null || userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        if (userService.addFollow(user.getUserId(), userId)) {
            // 更新session中的用户信息
            session.setAttribute("user", JSON.toJSONString(user));
            return "关注成功";
        }
        else {
            return "关注失败";
        }
    }

    @Override
    public void insertFollower(Follower follower) {
        userMapper.addFollower(follower);
    }

    @Override
    public void updateFollower(Follower follower) {
        userMapper.updateFollower(follower);
    }

    @Override
    public Following selectIfFollow(Integer userId, Integer userId1) {
        return userMapper.selectFollowing(userId, userId1);
    }

    @Override
    public Follower selectIfFollower(Integer userId, Integer userId1) {
        return userMapper.selectFollower(userId, userId1);
    }

    @Override
    public Integer selectFollowsCount(Integer userId) {
        return userMapper.selectFollowsCount(userId);
    }
}

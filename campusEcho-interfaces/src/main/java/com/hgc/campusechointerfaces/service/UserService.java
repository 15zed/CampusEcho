package com.hgc.campusechointerfaces.service;

import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;
import com.hgc.campusechomodel.entity.User;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
public interface UserService {

    User selectUser(String username);

    /**
     * 添加用户
     * @param user 用户信息
     * @return 数据库受影响行数
     */
    int add(User user);

    User selectById(Integer userId);




    /**
     * 添加关注
     * @param userId 关注人
     * @param userId1 被关注人
     * @return
     */
    boolean addFollow(Integer userId, Integer userId1);

    String generateUniqueId(User user);

    List<Following> selectFollows(Integer userId);


    boolean unFollow(Integer userId1, Integer userId2);



    List<Follower> selectFans(Integer userId,long offset,long limit);




    /**
     * 登录
     * @param modelAndView
     * @param request 请求
     * @param user 请求传来的用户信息
     * @return
     */
    ModelAndView doLogin(ModelAndView modelAndView, HttpServletRequest request, User user);

    /**
     * 登录处理
     * @param selectUser 数据库或者缓存中的用户信息
     * @param user 请求传入的用户信息
     * @param request 请求
     * @param jedis 连接对象
     * @param modelAndView
     */
    void choose(User selectUser, User user, HttpServletRequest request, Jedis jedis, ModelAndView modelAndView);

    /**
     * 处理用户传的文件 包括头像和帖子中的照片
     * @param file 用户上传的文件
     * @return 文件名
     */
    String handFile(MultipartFile file);

    /**
     * 去用户主页
     * @param userId 主页用户的id
     * @param modelAndView
     * @param request 请求
     */
    void toUserHomeById(Integer userId, ModelAndView modelAndView, HttpServletRequest request,int page,int size);

    /**
     * 处理关注逻辑
     * @param user 关注人
     * @param userId 被关注人id
     * @param session
     * @return
     */
    String follow(User user, Integer userId, HttpSession session);

    /**
     * 取消关注
     * @param user 取消人
     * @param userId 取消关注谁
     * @param session
     * @return
     */
    String cancel(User user, Integer userId, HttpSession session);

    void insertFollower(Follower follower);

    void updateFollower(Follower follower);

    Following selectIfFollow(Integer userId, Integer userId1);

    Follower selectIfFollower(Integer userId, Integer userId1);

    Integer selectFollowsCount(Integer userId);


//    User change(User user);

}

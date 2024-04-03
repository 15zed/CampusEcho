package com.hgc.school.service.impl;

import com.alibaba.fastjson.JSON;
import com.hgc.school.Constants.FileConstant;
import com.hgc.school.commons.ErrorCode;
import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.exception.BusinessException;
import com.hgc.school.mapper.UserMapper;
import com.hgc.school.service.InfoService;
import com.hgc.school.service.UserService;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import lombok.extern.slf4j.Slf4j;
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
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;

/**
 *
 */
@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Autowired
    @Lazy
    private UserService userService;
    @Autowired
    private InfoService infoService;
    @Autowired
    private UserMapper userMapper;
    @Resource
    private JedisPool jedisPool;

    @Override
    public User selectUser(String username) {
        if (username == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.selectUser(username);
    }

    @Override
    public int add(User user) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.addUser(user);
    }

    @Override
    public User selectById(Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.selectById(userId);
    }

    @Override
    public boolean hasUserLikedPost(Integer userId, Integer id) {
        if (userId == null || id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectById(userId);
        if (user != null) {
            String likeList = user.getLikelist();
            // 检查用户点赞列表中是否包含指定帖子的 ID
            return likeList != null && Arrays.asList(likeList.split(",")).contains(id.toString());
            //这里用户的点赞列表存放是的帖子的id 以 ， 分隔。  Arrays.asList()方法可以把数组转成集合 调用集合的contains()方法可以优雅的判断变量是否在一个字符串中
            //直接for循环也简单粗暴
        }
        return false;
    }

    @Async("threadPoolExecutor")
    @Override
    public Future<Integer> addPostToUserLikes(Integer userId, Integer id) {
        if (userId == null || id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException(ErrorCode.NULL_ERROR);
        String likelist = user.getLikelist();
        //likelist.isEmpty()代表空字符串
        if (likelist == null || likelist.isEmpty()) {
            user.setLikelist(id.toString());
        } else {
            user.setLikelist(likelist + "," + id.toString());
        }
        return new AsyncResult<>(userMapper.updateLikeList(user));
    }

    /**
     * 关注
     *
     * @param userId1 关注人
     * @param userId2 被关注人
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public boolean addFollow(Integer userId1, Integer userId2) {
        if (userId1 == null || userId2 == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user1 = userMapper.selectById(userId1);
        User user2 = userMapper.selectById(userId2);
        if (user1 != null && user2 != null) {
            String follows = user1.getFollows();
            String fans = user2.getFans();
            if (follows == null || follows.isEmpty()) {
                user1.setFollows(userId2.toString());
            } else if (!Arrays.asList(follows.split(",")).contains(userId2.toString())) {
                user1.setFollows(follows + "," + userId2);
            }
            if (fans == null || fans.isEmpty()) {
                user2.setFans(userId1.toString());
            } else if (!Arrays.asList(fans.split(",")).contains(userId1.toString())) {
                user2.setFans(fans + "," + userId1);
            }
            userMapper.updateFollows(user1);
            userMapper.updateFans(user2);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public List<User> selectFollows(Integer userId) {
        if (userId == null || userId == 0) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Jedis jedis = jedisPool.getResource();
        String jsUser = jedis.get("user:" + userId);
        if (jsUser == null) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return new ArrayList<>();
            }
            return getFollowList(user);
        }
        User user = JSON.parseObject(jsUser, User.class);
        jedis.close();
        return getFollowList(user);
    }

    @Override
    public List<User> getFollowList(User user) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        ArrayList<User> userList = new ArrayList<>();
        String userFollows = user.getFollows();
        if (userFollows != null) {
            String[] follows = userFollows.split(",");
            if (follows.length > 0 && !Objects.equals(follows[0], "")) {
                for (String follow : follows) {
                    User user1 = userMapper.selectById(Integer.parseInt(follow));
                    userList.add(user1);
                }
            }
        }
        log.info(userList.toString());
        return userList;
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
            // 更新关注列表
            user.setFollows(userService.getFollows(user.getUserId()));
            //删除缓存
            Jedis jedis = jedisPool.getResource();
            jedis.del("user:" + user.getUserId());
            jedis.del("user:" + user.getUsername());
            jedis.close();
            // 更新session中的用户信息
            session.setAttribute("user", JSON.toJSONString(user));
            return "取消成功";
        } else
            return "取消失败";
    }


    /**
     * 取消关注
     *
     * @param userId1 操作人
     * @param userId2 取消谁
     * @return
     */
    @Transactional(propagation = Propagation.REQUIRED, rollbackFor = Exception.class)
    @Override
    public boolean unFollow(Integer userId1, Integer userId2) {
        if (userId1 == null || userId2 == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        User user1 = userMapper.selectById(userId1);
        User user2 = userMapper.selectById(userId2);
        if (user1 != null && user2 != null) {
            String follows = user1.getFollows();
            String fans = user2.getFans();
            StringBuffer sb = new StringBuffer();
            if (follows != null && !follows.isEmpty()) {
                String[] array = follows.split(",");
                for (String s : array) {
                    int i = Integer.parseInt(s);
                    if (i != userId2) {
                        sb.append(i);
                        sb.append(",");
                    }
                }
                if (sb.length() > 0) {
                    sb = sb.deleteCharAt(sb.length() - 1);
                }
                user1.setFollows(sb.toString());
                sb.setLength(0);
            }
            if (fans != null && !fans.isEmpty()) {
                String[] array = fans.split(",");
                for (String s : array) {
                    int i = Integer.parseInt(s);
                    if (i != userId1) {
                        sb.append(i);
                        sb.append(",");
                    }
                }
                if (sb.length() > 0) {
                    sb = sb.deleteCharAt(sb.length() - 1);
                }
                user2.setFans(sb.toString());
            }
            userMapper.updateFollows(user1);
            userMapper.updateFans(user2);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String getFollows(Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return userMapper.selectById(userId).getFollows();
    }

    @Override
    public List<User> selectFans(Integer userId) {
        if (userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Jedis jedis = jedisPool.getResource();
        String jsUser = jedis.get("user:" + userId);
        if (jsUser == null) {
            User user = userMapper.selectById(userId);
            if (user == null) {
                return new ArrayList<>();
            }
            return getFansList(user);
        }
        User user = JSON.parseObject(jsUser, User.class);
        return getFansList(user);
    }

    @Override
    public List<User> getFansList(User user) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String userFans = user.getFans();
        ArrayList<User> userList = new ArrayList<>();
        if (userFans != null) {
            String[] fans = userFans.split(",");
            if (fans.length > 0 && !Objects.equals(fans[0], "")) {
                for (String fan : fans) {
                    User user1 = userMapper.selectById(Integer.parseInt(fan));
                    userList.add(user1);
                }
            }
        }
        log.info("粉丝列表：" + userList.toString());
        return userList;
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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return filename;
    }

    /**
     * 去用户主页
     *
     * @param userId       主页用户的id
     * @param modelAndView
     * @param request      请求
     */
    @Override
    public void toUserHomeById(Integer userId, ModelAndView modelAndView, HttpServletRequest request)  {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
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
        List<InfoWithCommentsDTO> dtoList = infoService.getDtoByUserId(userId);
        String jsonUser = (String) request.getSession().getAttribute("user");
        User sessionUser = JSON.parseObject(jsonUser, User.class);
        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.addObject("user", user);
        modelAndView.addObject("dtoList", dtoList);
        modelAndView.setViewName("userhome");
        jedis.close();
    }

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
            // 更新关注列表
            user.setFollows(userService.getFollows(user.getUserId()));
            //删除缓存
            Jedis jedis = jedisPool.getResource();
            jedis.del("user:" + user.getUserId());
            jedis.del("user:" + user.getUsername());
            jedis.close();
            // 更新session中的用户信息
            session.setAttribute("user", JSON.toJSONString(user));
            return "关注成功";
        } else
            return "关注失败";
    }
}

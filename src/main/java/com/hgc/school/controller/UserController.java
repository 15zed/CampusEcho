package com.hgc.school.controller;

import com.alibaba.fastjson.JSON;
import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.service.CommentService;
import com.hgc.school.service.InfoService;
import com.hgc.school.service.UserService;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 *
 */
@RestController
@RequestMapping("/user")
public class UserController {
    @Value("${school.filePrefix}")
    private String prefix;
    @Autowired
    private UserService userService;
    @Autowired
    private InfoService infoService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private JedisPool jedisPool;

    /**
     * 去往登录页
     *
     * @return
     */
    @GetMapping("/forward")
    public ModelAndView forward() {
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("login");
        return modelAndView;
    }

    /**
     * 登录
     *
     * @param request
     * @param user
     * @return
     */
    @PostMapping("/login")
    public ModelAndView login(HttpServletRequest request, User user) {
//       ModelAndView modelAndView = new ModelAndView();
//       User selectUser = userService.selectUser(user.getUsername());
//        if(selectUser == null ){
//            modelAndView.setViewName("register");
//            return modelAndView;
//        }else if(selectUser.getPassword().equals(user.getPassword())){
//            if(selectUser.getStatus() != 0) {
//                request.getSession().setAttribute("user", JSON.toJSONString(selectUser));
//                modelAndView.setViewName("welcome");
//                return modelAndView;
//            }else {
//                modelAndView.addObject("info","账号被禁用");
//                modelAndView.setViewName("login");
//                return modelAndView;
//            }
//        }else {
//            modelAndView.addObject("info","密码错误");
//            modelAndView.setViewName("login");
//            return modelAndView;
//        }

        ModelAndView modelAndView = new ModelAndView();
        Jedis jedis = jedisPool.getResource();
        String userId = jedis.get("user:" + user.getUsername());
        String jsonUser = jedis.get("user:" + userId);
        if (jsonUser == null || jsonUser.isEmpty()) {
            User selectUser = userService.selectUser(user.getUsername());
            if (selectUser == null) {
                modelAndView.setViewName("register");
                return modelAndView;
            } else if (selectUser.getPassword().equals(user.getPassword())) {
                jedis.set("user:"+selectUser.getUserId(),JSON.toJSONString(selectUser));
                if (selectUser.getStatus() != 0) {
                    request.getSession().setAttribute("user", JSON.toJSONString(selectUser));
                    modelAndView.setViewName("welcome");
                } else {
                    modelAndView.addObject("info", "账号被禁用");
                    modelAndView.setViewName("login");
                }
                jedis.close();
                return modelAndView;
            } else {
                jedis.set("user:"+selectUser.getUserId(),JSON.toJSONString(selectUser));
                modelAndView.addObject("info", "密码错误");
                modelAndView.setViewName("login");
                jedis.close();
                return modelAndView;
            }
        }
        User selectUser = JSON.parseObject(jsonUser, User.class);
        if (selectUser.getPassword().equals(user.getPassword())) {
            if (selectUser.getStatus() != 0) {
                request.getSession().setAttribute("user", JSON.toJSONString(selectUser));
                modelAndView.setViewName("welcome");
            } else {
                modelAndView.addObject("info", "账号被禁用");
                modelAndView.setViewName("login");
            }
        } else {
            modelAndView.addObject("info", "密码错误");
            modelAndView.setViewName("login");
        }
        jedis.close();
        return modelAndView;
    }

    /**
     * 注册
     *
     * @param user
     * @param file
     * @return
     * @throws IOException
     */
    @PostMapping("/register")
    public ModelAndView register(User user, @RequestParam("file") MultipartFile file) throws IOException
    {
        ModelAndView modelAndView = new ModelAndView();
        String originalFilename = file.getOriginalFilename();
        //截取原始文件名后缀 .jpg  .png  .....
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //拼接成最后的文件名
        String filename = UUID.randomUUID() + suffix;
        file.transferTo(new File(prefix + filename));
        user.setHead(filename);
        userService.add(user);
        modelAndView.setViewName("login");
        modelAndView.addObject("info", "注册成功");
        return modelAndView;
    }

    /**
     * 去主页
     *
     * @param userId
     * @return
     */
    @GetMapping("/{userId}")
    public ModelAndView toUserHome(@PathVariable("userId") Integer userId, HttpServletRequest request) {
//        ModelAndView modelAndView = new ModelAndView();
//        List<InfoWithCommentsDTO> dtoList = new ArrayList<>();
//        User user = userService.selectById(userId);
//        List<Info> infoList = infoService.selectInfos(userId);
//        for (Info info : infoList) {
//            InfoWithCommentsDTO dto = new InfoWithCommentsDTO();
//            List<CommentInfo> commentsList = commentService.selectByInfoId(info.getId());
//            dto.setInfo(info);
//            dto.setComments(commentsList);
//            dtoList.add(dto);
//        }
//        String jsonUser = (String) request.getSession().getAttribute("user");
//        User sessionUser = JSON.parseObject(jsonUser, User.class);
//        modelAndView.addObject("sessionUser", sessionUser);
//        modelAndView.addObject("user", user);
//        modelAndView.addObject("dtoList", dtoList);
//        modelAndView.setViewName("userhome");
//        return modelAndView;

        ModelAndView modelAndView = new ModelAndView();
        List<InfoWithCommentsDTO> dtoList = new ArrayList<>();
        Jedis jedis = jedisPool.getResource();
        String jUser = jedis.get("user:" + userId);
        if(jUser == null || jUser.isEmpty()){
            User user = userService.selectById(userId);
            jedis.set("user:"+user.getUserId(),JSON.toJSONString(user));
        }
        User user = JSON.parseObject(jUser, User.class);
        List<Info> infoList = new ArrayList<>();
        List<String> infoIdList = jedis.lrange("info:userId:" + userId, 0, -1);
        for (String infoId : infoIdList) {
            String jsonInfo = jedis.get("info:" + infoId);
            Info info = JSON.parseObject(jsonInfo, Info.class);
            infoList.add(info);
        }
        for (Info info : infoList) {
            InfoWithCommentsDTO dto = new InfoWithCommentsDTO();
            List<CommentInfo> commentsList = new ArrayList<>();
            List<String> commentIdList = jedis.lrange("comment:pubId:" + info.getId(), 0, -1);
            for (String  commentId: commentIdList) {
                String jsComment = jedis.get("comment:" + commentId);
                CommentInfo commentInfo = JSON.parseObject(jsComment, CommentInfo.class);
                commentsList.add(commentInfo);
            }
            dto.setInfo(info);
            dto.setComments(commentsList);
            dtoList.add(dto);
        }
        String jsonUser = (String) request.getSession().getAttribute("user");
        User sessionUser = JSON.parseObject(jsonUser, User.class);
        modelAndView.addObject("sessionUser", sessionUser);
        modelAndView.addObject("user", user);
        modelAndView.addObject("dtoList", dtoList);
        modelAndView.setViewName("userhome");
        jedis.close();
        return modelAndView;
    }

    /**
     * 退出登录
     *
     * @param request
     * @return
     */
    @GetMapping("/logout")
    public ModelAndView logout(HttpServletRequest request) {
        ModelAndView modelAndView = new ModelAndView();
        request.getSession().removeAttribute("user");
        modelAndView.setViewName("login");
        return modelAndView;
    }

}

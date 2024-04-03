package com.hgc.school.controller;


import com.hgc.school.commons.ErrorCode;
import com.hgc.school.exception.BusinessException;
import com.hgc.school.service.UserService;
import com.hgc.school.vo.User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.UnsupportedEncodingException;

/**
 *
 */
@Controller
@RequestMapping("/user")
public class UserController {
    @Resource
    private UserService userService;


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
     *
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
     *
     * @param user 请求传来的用户信息
     * @param file 用户的头像
     * @return
     */
    @PostMapping("/register")
    public ModelAndView register(User user, @RequestParam("file") MultipartFile file) {
        if (user == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        ModelAndView modelAndView = new ModelAndView();
        if (!file.isEmpty()) {
            String filename = userService.handFile(file);
            String newName = filename.replaceAll("''", "");
            user.setHead(newName);
        }
        userService.add(user);
        modelAndView.setViewName("login");
        modelAndView.addObject("info", "注册成功");
        return modelAndView;
    }

    /**
     * 去某个用户主页
     *
     *
     * @param userId 目标主页用户的id
     * @return
     */
    @GetMapping("/{userId}")
    public ModelAndView toUserHome(@PathVariable("userId") Integer userId, HttpServletRequest request) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        ModelAndView modelAndView = new ModelAndView();
        userService.toUserHomeById(userId, modelAndView, request);
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

}

package com.hgc.campusechocountservice.controller;

import com.alibaba.fastjson.JSON;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechomodel.entity.User;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;

/**
 *
 */
@RestController
@RequestMapping("/count")
public class CountController {
    @Resource
    private CountService countService;
    @Resource
    private RabbitTemplate rabbitTemplate;


    /**
     * 获取帖子点赞数
     *
     * @param infoId 帖子id
     * @return 帖子点赞数
     */
    @GetMapping("/getLikes/{infoId}")
    public Long getLikes(@PathVariable("infoId") Integer infoId) {
        //数据冷热分离后，需要先从redis查，redis没有再去MySQL查，并写入缓存
        return countService.getLikes(infoId);
    }

    /**
     * 获取帖子评论数
     *
     * @param infoId 帖子id
     * @return 帖子评论数
     */
    @GetMapping("/getComments/{infoId}")
    public Long getComments(@PathVariable("infoId") Integer infoId) {
        //数据冷热分离后，需要先从redis查，redis没有再去MySQL查，并写入缓存
        return countService.getComments(infoId);
    }

    /**
     * 获取用户关注数
     *
     * @param userId 用户id
     * @return 用户关注数
     */
    @GetMapping("/user/follow/{userId}")
    public Long getFollows(@PathVariable("userId") Integer userId) {
        //数据冷热分离后，需要先从redis查，redis没有再去MySQL查，并写入缓存
        return countService.getFollows(userId);
    }

    /**
     * 获取用户粉丝数
     *
     * @param userId 用户id
     * @return 用户粉丝数
     */
    @GetMapping("/user/fans/{userId}")
    public Long getFans(@PathVariable("userId") Integer userId) {
        //数据冷热分离后，需要先从redis查，redis没有再去MySQL查，并写入缓存
        return countService.getFans(userId);
    }

    /**
     * 增加帖子点赞数
     *
     * @param infoId 帖子id
     * @return
     */
    @GetMapping("/addLikes/{infoId}")
    public String addLikes(@PathVariable("infoId") Integer infoId, HttpSession session) {
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        Integer userId = user.getUserId();
        ArrayList<Integer> list = new ArrayList<>();
        list.add(infoId);
        list.add(userId);
        list.add(1);
        // 缓存和MySQL和榜单服务消费，MySQL中记录用户的点赞记录，推荐会使用到
        rabbitTemplate.convertAndSend("likes-fanoutExchange", "", list);
        return "ok";
    }

    /**
     * 减少帖子点赞数
     *
     * @param infoId 帖子id
     * @return
     */
    @GetMapping("/reduceLikes/{infoId}")
    public String reduceLikes(@PathVariable("infoId") Integer infoId, HttpSession session) {
        if (infoId == null) throw new BusinessException(ErrorCode.NULL_ERROR, "帖子id不能为空");
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        Integer userId = user.getUserId();
        ArrayList<Integer> list = new ArrayList<>();
        list.add(infoId);
        list.add(userId);
        list.add(0);
        // 缓存和MySQL和榜单服务消费，MySQL中记录用户的点赞记录，推荐会使用到
        rabbitTemplate.convertAndSend("likes-fanoutExchange", "", list);
        return "ok";
    }

    /**
     * 增加帖子评论数
     *
     * @param infoId 帖子id
     * @return
     */
    @GetMapping("/addComments/{infoId}")
    public String addComments(@PathVariable("infoId") Integer infoId) {
        if (infoId == null) throw new BusinessException(ErrorCode.NULL_ERROR, "帖子id不能为空");
        ArrayList<Integer> list = new ArrayList<>();
        list.add(infoId);
        list.add(1);
        // 缓存消费
        rabbitTemplate.convertAndSend("comments-fanoutExchange", "", list);
        return "ok";
    }

    /**
     * 减少帖子评论数
     *
     * @param infoId 帖子id
     * @return
     */
    @GetMapping("/reduceComments/{infoId}")
    public String reduceComments(@PathVariable("infoId") Integer infoId) {
        if (infoId == null) throw new BusinessException(ErrorCode.NULL_ERROR, "帖子id不能为空");
        ArrayList<Integer> list = new ArrayList<>();
        list.add(infoId);
        list.add(0);
        // 缓存消费
        rabbitTemplate.convertAndSend("comments-fanoutExchange", "", list);
        return "ok";
    }

    /**
     * 增加用户关注数
     *
     * @param userId 用户id
     * @return
     */
    @GetMapping("/addFollows/{userId}")
    public String addFollows(@PathVariable("userId") Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.NULL_ERROR, "用户id不能为空");
        ArrayList<Integer> list = new ArrayList<>();
        list.add(userId);
        list.add(1);
        // 缓存消费
        rabbitTemplate.convertAndSend("count-exchange", "count.Follows", list);
        return "ok";
    }

    /**
     * 减少用户关注数
     *
     * @param userId 用户id
     * @return
     */
    @GetMapping("/reduceFollows/{userId}")
    public String reduceFollows(@PathVariable("userId") Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.NULL_ERROR, "用户id不能为空");
        ArrayList<Integer> list = new ArrayList<>();
        list.add(userId);
        list.add(0);
        // 缓存消费
        rabbitTemplate.convertAndSend("count-exchange", "count.Follows", userId);
        return "ok";
    }

    /**
     * 增加用户粉丝数
     *
     * @param userId 用户id
     * @return
     */
    @GetMapping("/addFans/{userId}")
    public String addFans(@PathVariable("userId") Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.NULL_ERROR, "用户id不能为空");
        ArrayList<Integer> list = new ArrayList<>();
        list.add(userId);
        list.add(1);
        // 缓存消费
        rabbitTemplate.convertAndSend("count-exchange", "count.Fans", list);
        return "ok";
    }

    /**
     * 减少用户粉丝数
     *
     * @param userId 用户id
     * @return
     */
    @GetMapping("/reduceFans/{userId}")
    public String reduceFans(@PathVariable("userId") Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.NULL_ERROR, "用户id不能为空");
        ArrayList<Integer> list = new ArrayList<>();
        list.add(userId);
        list.add(0);
        rabbitTemplate.convertAndSend("count-exchange", "count.Fans", list);
        return "ok";
    }
}

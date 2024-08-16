package com.hgc.campusechocommentservice.controller;

import com.alibaba.fastjson.JSON;
import com.hgc.campusechocommentservice.service.RedisService;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechomodel.entity.CommentInfo;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/comment")
public class CommentController {
    @Resource
    private CommentService commentService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private RedisService redisService;

    /**
     * 发评论
     * ✔
     * @param comment
     * @return
     */
    @PostMapping("/addcomment")
    public String addComment(@RequestBody CommentInfo comment) {
        if (comment == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String uniqueId = commentService.generateUniqueId(comment);
        if (!redisService.setUniqueId(uniqueId)) {
            return "请勿重复提交";
        }
        // 直接发消息到MQ,本地消费时再插入MySQL，同步ES数据,评论是一个读写都是高并发的场景，所以这里使用MQ
        rabbitTemplate.convertAndSend("addcomt-fanoutExchange", "", comment);
        return JSON.toJSONString(comment);
    }

    /**
     * 删除某条评论
     * ✔
     * @param comment
     * @return
     */
    @DeleteMapping("/deletecomment")
    public String deleteOneComment(@RequestBody CommentInfo comment) {
        if(comment == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // MySQL消费，ES消费，本地缓存消费,计数服务消费，榜单服务消费
        rabbitTemplate.convertAndSend("delOneComment-fanoutExchange", "", comment);
        return "ok";
    }

    /**
     * 删除帖子相关所有评论
     * ✔
     * @param infoId
     * @return
     */
    @DeleteMapping("/deletecomment/{infoId}")
    public String deleteAllComment(@PathVariable Integer infoId){
        if(infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<Integer> commentIdList = commentService.selectIdsByInfoId(infoId);
        List<Object> list = new ArrayList<>();
        list.add(infoId);
        list.add(commentIdList);
        // MySQL消费，ES消费，本地缓存消费,计数服务消费，榜单服务消费
        rabbitTemplate.convertAndSend("delAllComment-fanoutExchange","",list);
        return "ok";
    }

    /**
     * 获取帖子下的评论，分页,每一页固定展示10条评论
     * ✔
     * @param infoId
     * @param pageNum
     * @return
     */
    @GetMapping("/getcomment/{infoId}")
    public String getComment(@PathVariable Integer infoId, @RequestParam(defaultValue = "1") Integer pageNum) {
        if(infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<CommentInfo> comments = commentService.selectPage(infoId,pageNum);
        return JSON.toJSONString(comments);
    }
}

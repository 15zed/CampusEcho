package com.hgc.campusechocountservice.rabbitmq;

import com.hgc.campusechocountservice.controller.SseController;
import com.hgc.campusechocountservice.mapper.LikesMapper;
import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechomodel.entity.CommentInfo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 对写请求进行消费
 */
@Component
public class Consumer {
    @Autowired
    private CountService countService;
    @Autowired
    private LikesMapper likesMapper;
    @Autowired
    private SseController sseController;


    /**
     * 帖子点赞数
     * @param list
     */
    @RabbitListener(queues = "local-likes.queue")
    public void consumeLikes(@Payload List<Integer> list) {
        Integer infoId = list.get(0);
        Integer userId = list.get(1);
        Integer type = list.get(2);
        //数据冷热分离后，需要确定数据位置，如果在redis中，直接更新，如果在MySQL中，更新后再写回redis，最后删除MySQL
        if(type == 1){
            //增加点赞数
            Long latestLikes = countService.addLikes(infoId);
            //mysql增加点赞记录
            likesMapper.insertLikes(infoId,userId);
            //前端需要获取消息处理的结果，更新页面。常见有这些方式：前端轮询、后端websocket推送、后端事件通知(SSE)、前端再次调用获取值的接口
            sseController.sendLikesUpdate(infoId,latestLikes);
        }
        if(type == 0){
            //减少点赞数
            Long latestLikes = countService.reduceLikes(infoId,userId);
            //mysql删除点赞记录
            likesMapper.deleteLikes(infoId,userId);
            sseController.sendLikesUpdate(infoId,latestLikes);
        }
    }

    /**
     * 帖子评论数
     * @param list
     */
    @RabbitListener(queues = "local-comments.queue")
    public void consumeComments(@Payload List<Integer> list) {
        Integer infoId = list.get(0);
        Integer type = list.get(1);
        //数据冷热分离后，需要确定数据位置，如果在redis中，直接更新，如果在MySQL中，更新后再写回redis，最后删除MySQL
        if(type == 1){
            //增加评论数
            Long latestComments = countService.addComments(infoId);
            //前端需要获取消息处理的结果，更新页面。常见有这些方式：前端轮询、后端websocket推送、后端事件通知(SSE)、前端再次调用获取值的接口
            sseController.sendCommentsUpdate(infoId,latestComments);
        }
        if(type == 0){
            //减少评论数
            Long latestComments = countService.reduceComments(infoId);
            sseController.sendCommentsUpdate(infoId,latestComments);
        }
    }

    /**
     * 用户关注数
     * @param list
     */
    @RabbitListener(queues = "follows-queue")
    public void consumeFollows(@Payload List<Integer> list) {
        Integer userId = list.get(0);
        Integer type = list.get(1);
        //数据冷热分离后，需要确定数据位置，如果在redis中，直接更新，如果在MySQL中，更新后再写回redis，最后删除MySQL
        if(type == 1){
            Long latestFollows = countService.addFollows(userId);
            //前端需要获取消息处理的结果，更新页面。常见有这些方式：前端轮询、后端websocket推送、后端事件通知(SSE)、前端再次调用获取值的接口
            sseController.sendFollowingUpdate(userId,latestFollows);
        }
        if(type == 0){
            Long latestFollows = countService.reduceFollows(userId);
            sseController.sendFollowingUpdate(userId,latestFollows);
        }
    }

    /**
     * 用户粉丝数
     * @param list
     */
    @RabbitListener(queues = "fans-queue")
    public void consumeFans(@Payload List<Integer> list) {
        Integer userId = list.get(0);
        Integer type = list.get(1);
        //数据冷热分离后，需要确定数据位置，如果在redis中，直接更新，如果在MySQL中，更新后再写回redis，最后删除MySQL
        if(type == 1){
            Long latestFans = countService.addFans(userId);
            //前端需要获取消息处理的结果，更新页面。常见有这些方式：前端轮询、后端websocket推送、后端事件通知(SSE)、前端再次调用获取值的接口
            sseController.sendFollowersUpdate(userId,latestFans);
        }
        if(type == 0){
            Long latestFans = countService.reduceFans(userId);
            sseController.sendFollowersUpdate(userId,latestFans);
        }
    }


    /**
     * 删除帖子
     *
     * @param list
     */
    @RabbitListener(queues = "cout-delPosts.queue")
    public void consumerDelete(@Payload List<Object> list){
        Integer infoId = (Integer) list.get(0);
        //计数服务删除帖子相关所有计数
        countService.deleteCountByInfoId(infoId);
    }

    /**
     * 监听评论队列
     * @param comment
     */
    @RabbitListener(queues = "cout-addCommnet.queue")
    public void consumer(@Payload CommentInfo comment) {
        //计数服务，评论数+1
        countService.addComments(comment.getCommentId());
    }

    /**
     * 删除某条评论
     * @param comment
     */
    @RabbitListener(queues = "cout-deleteOne.queue")
    public void deleteOneComment(@Payload CommentInfo comment) {
        //计数服务，评论数-1
        countService.reduceComments(comment.getPubId());
    }

    /**
     * 删除多条评论
     * @param list
     */
    @RabbitListener(queues = "cout-deleteAll.queue")
    public void deleteAllComment(@Payload List<Object> list) {
        Integer infoId = (Integer) list.get(0);
        //计数服务
        countService.deleteCountByInfoId(infoId);
    }
}

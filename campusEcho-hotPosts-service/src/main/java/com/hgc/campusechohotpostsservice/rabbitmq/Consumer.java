package com.hgc.campusechohotpostsservice.rabbitmq;


import com.hgc.campusechointerfaces.service.HotPostsService;
import com.hgc.campusechomodel.entity.CommentInfo;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 */
@Component
public class Consumer {
    @Autowired
    private HotPostsService hotPostsService;

    /**
     * 监听点赞队列,计数服务更新帖子点赞后，更新榜单
     * @param list
     */
    @RabbitListener(queues = "remote-likes.queue")
    public void consumeLikes(@Payload List<Integer> list) {
        Integer infoId = list.get(0);
        hotPostsService.updateHotPosts(infoId);
    }

    /**
     * 监听评论队列,计数服务更新帖子评论数后，更新榜单
     * @param list
     */
    @RabbitListener(queues = "remote-comments.queue")
    public void consumeComments(@Payload List<Integer> list) {
        Integer infoId = list.get(0);
        hotPostsService.updateHotPosts(infoId);
    }

    /**
     * 删除帖子
     *
     * @param list
     */
    @RabbitListener(queues = "hot-delPosts.queue")
    public void consumerDelete(@Payload List<Object> list){
        Integer infoId = (Integer) list.get(0);
        //榜单服务，更新榜单
        hotPostsService.deleteHotPosts(infoId);
    }

    /**
     * 添加评论
     * @param comment
     */
    @RabbitListener(queues = "hot-addCommnet.queue")
    public void consumer(@Payload CommentInfo comment) {
        //热度服务，更新热度
        hotPostsService.updateHotPosts(comment.getPubId());
    }

    /**
     * 删除某条评论
     * @param comment
     */
    @RabbitListener(queues = "hot-deleteOne.queue")
    public void deleteOneComment(@Payload CommentInfo comment) {
        //热度服务，更新热度
        hotPostsService.updateHotPosts(comment.getPubId());
    }

    /**
     * 删除多条评论
     * @param list
     */
    @RabbitListener(queues = "hot-deleteAll.queue")
    public void deleteAllComment(@Payload List<Object> list) {
        Integer infoId = (Integer) list.get(0);
        //热度服务，更新热度
        hotPostsService.updateHotPosts(infoId);
    }

}

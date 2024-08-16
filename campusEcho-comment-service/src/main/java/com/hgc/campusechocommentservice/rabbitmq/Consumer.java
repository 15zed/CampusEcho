package com.hgc.campusechocommentservice.rabbitmq;

import com.hgc.campusechocommentservice.mapper.CommentMapper;
import com.hgc.campusechocommentservice.schedule.CommentStorage;
import com.hgc.campusechocommentservice.service.RedisService;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechointerfaces.service.HotPostsService;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.sankuai.inf.leaf.service.SegmentService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Classname: Consumer
 * <p>
 * Version information:
 * <p>
 * User: zed
 * <p>
 * Date: 2024/7/27
 * <p>
 * Copyright notice:
 */
@Component
public class Consumer {
    @Autowired
    private CommentStorage commentStorage;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RedisService redisService;
    @DubboReference
    private CountService countService;
    @DubboReference
    private HotPostsService hotPostsService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private SegmentService segmentService;


    /**
     * 添加评论
     * @param comment
     */
    @RabbitListener(queues = "local-addCommnet.queue")
    public void consumer(@Payload CommentInfo comment) {
        //放入评论缓存对象，定时任务每10秒对收到的评论进行计算，把对同一条帖子id的评论进行合并，然后更新MySQL和缓存
        //生成分布式ID
        long distributeId = segmentService.getId("comment").getId();
        comment.setCommentId((int) distributeId);
        commentStorage.addComment(comment);
    }

    /**
     * 删除某条评论
     * @param comment
     */
    @RabbitListener(queues = "deleteOne.queue",concurrency = "10")
    public void deleteOneComment(@Payload CommentInfo comment) {
        //更新MySQL
        commentMapper.deleteByCommentId(comment.getCommentId());
        //更新缓存
        redisService.delete(comment.getCommentId());
    }

    /**
     * 删除多条评论
     * @param list
     */
    @RabbitListener(queues = "deleteAll.queue",concurrency = "10")
    public void deleteAllComment(@Payload List<Object> list) {
        List<Integer> commentIds = (List<Integer>) list.get(1);
        //更新MySQL
        commentMapper.deleteAll(commentIds);
        for (Integer commentId : commentIds) {
            //更新缓存
            redisService.delete(commentId);
        }
    }

    /**
     * 删除帖子
     *
     * @param list
     */
    @RabbitListener(queues = "comt-delPosts.queue",concurrency = "10")
    public void consumerDelete(@Payload List<Object> list){
        List<Integer> commentIdList = (List<Integer>) list.get(1);
        //调用评论服务，删除评论
        commentService.deleteAll(commentIdList);
    }
}

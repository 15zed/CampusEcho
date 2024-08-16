package com.hgc.campusechosearchandrecommendservice.rabbitmq;


import com.hgc.campusechocommon.constant.ESConstant;
import com.hgc.campusechointerfaces.service.ESService;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Info;
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
    private ESService esService;

    /**
     * 监听发帖队列
     * @param info
     */
    @RabbitListener(queues = "remote-addPosts.queue",concurrency = "10")
    public void consumerPost(@Payload Info info){
        //ES同步数据
        esService.add(ESConstant.INFO_INDEX, info);
    }

    /**
     * 监听删帖队列
     *
     * @param list
     */
    @RabbitListener(queues = "es-delPosts.queue",concurrency = "10")
    public void consumerDelete(@Payload List<Object> list){
        Integer infoId = (Integer) list.get(0);
        List<Integer> commentIdList = (List<Integer>) list.get(1);
        //ES服务同步数据
        esService.delete(String.valueOf(infoId), commentIdList);
    }

    /**
     * 监听评论队列
     * @param comment
     */
    @RabbitListener(queues = "ser-addComment.queue",concurrency = "10")
    public void consumer(@Payload CommentInfo comment) {
        //ES服务同步数据
        esService.add(ESConstant.COMMENT_INDEX, comment);
    }

    /**
     * 删除某条评论
     * @param comment
     */
    @RabbitListener(queues = "ser-deleteOne.queue",concurrency = "10")
    public void deleteOneComment(@Payload CommentInfo comment) {
        //ES服务同步数据
        esService.deleteOne(ESConstant.COMMENT_INDEX, comment.getCommentId());
    }

    /**
     * 删除多条评论
     * @param list
     */
    @RabbitListener(queues = "ser-deleteAll.queue",concurrency = "10")
    public void deleteAllComment(@Payload List<Object> list) {
        List<Integer> commentIds = (List<Integer>) list.get(1);
        //ES服务同步数据
        esService.deleteAll(ESConstant.COMMENT_INDEX, commentIds);
    }


}

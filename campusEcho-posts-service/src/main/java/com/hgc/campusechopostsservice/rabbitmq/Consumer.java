package com.hgc.campusechopostsservice.rabbitmq;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechointerfaces.service.HotPostsService;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechopostsservice.mapper.LikesMapper;
import com.hgc.campusechopostsservice.service.RedisService;
import com.sankuai.inf.leaf.service.SegmentService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;

/**
 * Classname: Consumer
 *
 */
@Component
public class Consumer {
    @Autowired
    private Cache<String, String> caffeineCache;
    @Resource
    private InfoService infoService;
    @Autowired
    private LikesMapper likesMapper;
    @Autowired
    private RedisService redisService;
    @DubboReference
    private CommentService commentService;
    @DubboReference
    private HotPostsService hotPostsService;
    @DubboReference
    private CountService countService;
    @Autowired
    private SegmentService segmentService;


    /**
     * 发帖
     * @param info
     */
    @RabbitListener(queues = "local-addPosts.queue")
    public void consumerPost(@Payload Info info){
        //更新MySQL
        //生成唯一id
        long id = segmentService.getId("info").getId();
        info.setId((int) id);
        infoService.add(info);
        //缓存预热
        Long fans = countService.getFans(info.getUserId());
        if(fans != null && fans > 50000){
            caffeineCache.put(String.valueOf(info.getId()), JSON.toJSONString(info));
        }
    }

    /**
     * 删除帖子
     *
     * @param list
     */
    @RabbitListener(queues = "local-delPosts.queue")
    public void consumerDelete(@Payload List<Object> list){
        Integer infoId = (Integer) list.get(0);
        //更新MySQL
        infoService.deleteInfoWithComments(infoId);
        //删除点赞记录表中关于该帖子的所有记录
        likesMapper.deleteLikesByInfoId(infoId);
        //删除缓存
        caffeineCache.invalidate(infoId);
        redisService.delete(infoId);
    }


}

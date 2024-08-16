package com.hgc.campusechocommentservice.schedule;

import com.hgc.campusechocommentservice.mapper.CommentMapper;
import com.hgc.campusechocommentservice.service.RedisService;
import com.hgc.campusechomodel.entity.CommentInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CommentProcessorSchedule {

    @Autowired
    private CommentStorage commentStorage;

    @Autowired
    private CommentMapper commentMapper;

    @Autowired
    private RedisService redisService;

    /**
     * 定时任务，每10秒执行一次
     * 对同一帖子id的评论进行批量更新
     */
    @Scheduled(fixedRate = 10000)
    public void processComments() {
        Map<Integer, List<CommentInfo>> commentsMap = new ConcurrentHashMap<>(commentStorage.getComments());

        commentsMap.forEach((pubId, comments) -> {
            if (!comments.isEmpty()) {
                // 批量更新MySQL
                commentMapper.batchAdd(comments);
                // 更新缓存
                comments.forEach(comment -> redisService.add(comment));
            }
        });

        // 清空临时存储的评论
        commentStorage.clearComments();
    }
}

package com.hgc.campusechocommentservice.schedule;

import com.hgc.campusechomodel.entity.CommentInfo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 评论临时存储，用来做合并插入的时候使用
 */
@Component
public class CommentStorage {
    /**
     * 评论存储
     * key: 帖子id
     * value: 评论列表
     */
    private final ConcurrentMap<Integer, List<CommentInfo>> commentMap = new ConcurrentHashMap<>();

    /**
     * 添加评论
     * @param comment
     */
    public void addComment(CommentInfo comment) {
        commentMap.computeIfAbsent(comment.getPubId(), k -> new ArrayList<>()).add(comment);
    }

    /**
     * 获取评论
     * @return
     */
    public ConcurrentMap<Integer, List<CommentInfo>> getComments() {
        return commentMap;
    }

    /**
     * 删除评论缓存
     */
    public void clearComments() {
        commentMap.clear();
    }
}

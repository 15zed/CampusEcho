package com.hgc.campusechomodel.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 *  评论类 和数据库的评论表对应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentInfo implements Serializable {

    /**
     * 评论id
     */
    private Integer commentId;
    /**
     * 评论用户的id
     */
    private Integer userId;
    /**
     * 被评论的帖子id
     */
    private Integer pubId;
    /**
     * 评论的内容
     */
    private String text;
    /**
     * 如果是对用户评论的回复，被回复用户的id
     */
    private Integer replyUserId;
    /**
     * 如果是对用户评论的回复，被回复评论的id
     */
    private Integer replyCommentId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /**
     * 评论时间
     */
    private LocalDateTime time;

    @Override
    public String toString() {
        return "CommentInfo{" +
                "userId=" + userId +
                ", pubId=" + pubId +
                ", text='" + text + '\'' +
                ", replyUserId=" + replyUserId +
                ", replyCommentId=" + replyCommentId +
                '}';
    }
}

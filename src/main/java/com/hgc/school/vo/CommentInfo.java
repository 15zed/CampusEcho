package com.hgc.school.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 *  评论类 和数据库的评论表对应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommentInfo {
    /**
     * 主键
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

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /**
     * 评论时间
     */
    private LocalDateTime time;
}

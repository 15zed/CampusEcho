package com.hgc.school.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;


/**
 * 帖子类，和数据库的帖子表对应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Info {
    /**
     * 主键
     */
    private Integer id;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    /**
     * 发帖时间
     */
    private LocalDateTime time;
    /**
     * 帖子中包含的图片
     */
    private String[] img;
    /**
     * 帖子内容
     */
    private String text;
    /**
     * 发帖人id
     */
    private Integer userId;
    /**
     * 发帖人头像
     */
    private String avatar;
    /**
     * 点赞数
     */
    private Integer likes;
    /**
     * 评论数
     */
    private Integer comments;
    /**
     * 帖子的分类
     */
    private String category;

}

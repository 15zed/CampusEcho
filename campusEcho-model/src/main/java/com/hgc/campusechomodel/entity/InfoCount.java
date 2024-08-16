package com.hgc.campusechomodel.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * User: zed
 * Date: 2024/8/8
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfoCount implements Serializable {
    /**
     * 主键
     */
    private Integer id;
    /**
     * 帖子id
     */
    private String infoId;
    /**
     * 点赞数量
     */
    private Integer likeCount;
    /**
     * 评论数量
     */
    private Integer commentCount;
    /**
     * 更新时间
     */
    private Integer time;
}

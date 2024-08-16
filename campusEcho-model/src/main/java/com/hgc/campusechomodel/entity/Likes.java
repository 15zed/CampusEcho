package com.hgc.campusechomodel.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 点赞类，和数据库的点赞表对应
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Likes implements Serializable {
    /**
     * 主键
     */
    private Integer id;
    /**
     * 用户id
     */
    private Integer userId;
    /**
     * 帖子id
     */
    private Integer infoId;
    /**
     * 点赞时间
     */
    private LocalDateTime time;
}

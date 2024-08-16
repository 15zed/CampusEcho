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
public class UserCount implements Serializable {
    /**
     * 主键
     */
    private Integer id;
    /**
     * 用户id
     */
    private Integer userId;
    /**
     * 关注数
     */
    private Integer followCount;
    /**
     * 粉丝数
     */
    private Integer fansCount;
    /**
     * 更新时间
     */
    private Integer time;

}

package com.hgc.campusechomodel.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;


/**
 * 关注表
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Following implements Serializable {

    /**
     * 主键
     */
    private Integer id;
    /**
     * 关注者id
     */
    private Integer fromUserId;
    /**
     * 被关注者id
     */
    private Integer toUserId;
    /**
     * 关注关系：1：正在关注，2：取消关注
     */
    private Integer type;
    /**
     * 记录修改时间
     */
    private Integer updateTime;

    @Override
    public String toString() {
        return "following{" +
                "fromUserId=" + fromUserId +
                ", toUserId=" + toUserId +
                ", type=" + type +
                ", updateTime=" + updateTime +
                '}';
    }
}

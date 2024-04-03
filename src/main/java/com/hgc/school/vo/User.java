package com.hgc.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户类，和数据库的用户表对应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    /**
     * 主键
     */
    private Integer userId;
    /**
     * 头像
     */
    private String head;
    /**
     * 姓名
     */
    private String username;
    /**
     * 密码
     */
    private String password;
    /**
     * 性别
     */
    private String sex;
    /**
     * 地区
     */
    private String area;
    /**
     * 联系方式
     */
    private String contact;
    /**
     * 用户状态 1 正常 0 封号
     */
    private Integer status;
    /**
     * 点赞列表
     */
    private String likelist;
    /**
     * 关注列表
     */
    private String follows;
    /**
     * 粉丝列表
     */
    private String fans;
}

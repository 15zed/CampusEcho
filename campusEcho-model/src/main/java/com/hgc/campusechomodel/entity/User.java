package com.hgc.campusechomodel.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 用户类，和数据库的用户表对应
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
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
     * 创建时间
     */
    private LocalDateTime createTime;
    /**
     * 最近登录时间
     */
    private LocalDateTime loginTime;

    @Override
    public String toString() {
        return "User{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", sex='" + sex + '\'' +
                ", area='" + area + '\'' +
                ", contact='" + contact + '\'' +
                '}';
    }
}

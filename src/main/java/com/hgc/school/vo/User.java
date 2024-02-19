package com.hgc.school.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 *
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Integer userId;
    private String head;
    private String username;
    private String password;
    private String sex;
    private String area;
    private String contact;
    private Integer status;
    private String likelist;
    private String follows;
    private String fans;
}

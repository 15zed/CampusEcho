package com.hgc.school.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;


/**
 *
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Info {
    private Integer id;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime time;
    private String[] img;
    private String text;
    private Integer userId;
    private String avatar;
    private Integer likes;
    private Integer comments;
    private String category;

//    public void setImg(String p_img) {
//        this.img = p_img.split(",");
//    }
}

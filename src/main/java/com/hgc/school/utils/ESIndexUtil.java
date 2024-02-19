package com.hgc.school.utils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@Getter
public class ESIndexUtil {
    public static String USER_INDEX = "user";
    public static String COMMENT_INDEX = "comment";
    public static String INFO_INDEX = "info";

}

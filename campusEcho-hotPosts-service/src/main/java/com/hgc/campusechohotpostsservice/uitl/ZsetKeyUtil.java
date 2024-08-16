package com.hgc.campusechohotpostsservice.uitl;

import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.constant.HotPostsConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.WeekFields;
import java.util.Locale;

/**
 * Classname: ZsetKeyUtil
 * <p>
 * Version information:
 * <p>
 * User: zed
 * <p>
 * Date: 2024/7/28
 * <p>
 * Copyright notice:
 */
@Slf4j
public class ZsetKeyUtil {

    public static String getKey(String prefixType){
         if(prefixType.equals(HotPostsConstant.HOUR)){
             // 获取当前时间
             LocalDateTime now = LocalDateTime.now();
             // 获取当前时间是一天的第几个小时(0~23) 排行榜设置从1开始，所以+1
             int hourOfDay = now.getHour() + 1;
             return HotPostsConstant.HOUR + hourOfDay;
         }
         if(prefixType.equals(HotPostsConstant.DAY)){
             // 获取当前时间
             LocalDateTime now = LocalDateTime.now();
             // 获取当前时间是一年的第多少天(1~365，1~366)
             int dayOfYear = now.getDayOfYear();
             return HotPostsConstant.DAY + dayOfYear;
         }
         if(prefixType.equals(HotPostsConstant.WEEK)){
             // 获取当前时间
             LocalDateTime now = LocalDateTime.now();
             // 获取当前时间是一年的第多少周(1~52,1~53)
             int weekOfYear = now.get(WeekFields.of(Locale.CHINA).weekOfWeekBasedYear());
             return HotPostsConstant.WEEK + weekOfYear;
         }
         throw new BusinessException(ErrorCode.PARAMS_ERROR);
    }
}

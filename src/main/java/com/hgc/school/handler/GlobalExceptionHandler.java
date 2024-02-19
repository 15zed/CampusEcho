package com.hgc.school.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 *
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String exceptionHandler(Exception e){
        log.info("错误类型："+e);
        log.error(e.getMessage());
        return "myError";
    }
}

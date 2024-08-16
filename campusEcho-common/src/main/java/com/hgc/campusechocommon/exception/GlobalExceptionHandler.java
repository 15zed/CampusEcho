package com.hgc.campusechocommon.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 全局的异常处理
 */
@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public String exceptionHandler1(Exception e) {
        log.info("错误类型：" + e);
        log.error(e.getMessage());
        return "myError";
    }

    @ExceptionHandler(BusinessException.class)
    public String exceptionHandler2(Exception e) {
        log.info("错误类型：" + e);
        log.error(e.getMessage());
        return "myError";
    }
}

package com.hgc.campusechocommon.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * 自定义线程池
 */
@Configuration
public class MyThreadPool {

    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        return new ThreadPoolExecutor(
                50,
                100,
                60,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(30),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}

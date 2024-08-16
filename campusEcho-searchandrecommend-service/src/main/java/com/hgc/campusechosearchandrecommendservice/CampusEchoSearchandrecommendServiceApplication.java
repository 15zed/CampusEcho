package com.hgc.campusechosearchandrecommendservice;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;


@SpringBootApplication
@MapperScan("com.hgc.campusechosearchandrecommendservice.mapper")
@EnableScheduling
@ComponentScan("com.hgc")
@EnableRedisHttpSession
@EnableDubbo
public class CampusEchoSearchandrecommendServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEchoSearchandrecommendServiceApplication.class, args);
    }

}

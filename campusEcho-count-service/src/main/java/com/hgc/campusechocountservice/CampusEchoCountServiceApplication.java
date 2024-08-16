package com.hgc.campusechocountservice;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@EnableRedisHttpSession
@ComponentScan("com.hgc")
@MapperScan("com.hgc.campusechocountservice.mapper")
public class CampusEchoCountServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEchoCountServiceApplication.class, args);
    }

}

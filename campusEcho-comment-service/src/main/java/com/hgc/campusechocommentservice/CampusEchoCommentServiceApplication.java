package com.hgc.campusechocommentservice;

import com.sankuai.inf.leaf.plugin.annotation.EnableLeafServer;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@ComponentScan("com.hgc")
@EnableDubbo
@EnableRedisHttpSession
@MapperScan("com.hgc.campusechocommentservice.mapper")
@EnableLeafServer
public class CampusEchoCommentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEchoCommentServiceApplication.class, args);
    }

}

package com.hgc.campusechopostsservice;

import com.sankuai.inf.leaf.plugin.annotation.EnableLeafServer;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@SpringBootApplication
@EnableRedisHttpSession
@ComponentScan("com.hgc")
@EnableDubbo
@EnableCaching
@MapperScan("com.hgc.campusechopostsservice.mapper")
@EnableLeafServer
public class CampusEchoPostsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CampusEchoPostsServiceApplication.class, args);
    }

}

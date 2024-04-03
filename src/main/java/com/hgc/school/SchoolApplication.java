package com.hgc.school;


import com.alibaba.fastjson.JSON;
import com.hgc.school.filter.LoginCheckFilter;
import com.hgc.school.mapper.CommentMapper;
import com.hgc.school.mapper.InfoMapper;
import com.hgc.school.mapper.UserMapper;
import com.hgc.school.service.InfoService;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.xcontent.XContentType;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.*;
import java.io.IOException;
import java.util.List;


@SpringBootApplication
@EnableTransactionManagement
@MapperScan("com.hgc.school.mapper")
@EnableScheduling
@ServletComponentScan
@EnableAsync
public class SchoolApplication implements InitializingBean, DisposableBean {
    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private InfoMapper infoMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private UserMapper userMapper;

    public static void main(String[] args) {
        SpringApplication.run(SchoolApplication.class, args);
    }

    /**
     * 将一部分用户数据放到redis，将所有的帖子和评论放到ES
     *
     * @throws Exception
     */
    @Override
    public void afterPropertiesSet() throws Exception {
        BulkRequest request = new BulkRequest();
        request.timeout("300m");
        Jedis jedis = jedisPool.getResource();
        List<CommentInfo> commentList = commentMapper.selectAll();
        List<Info> infoList = infoMapper.selectAll();
        List<User> userList = userMapper.selectPart();
        for (CommentInfo comment : commentList) {
            String jsonComment = JSON.toJSONString(comment);
            request.add(new IndexRequest("comment").id(String.valueOf(comment.getCommentId())).source(jsonComment, XContentType.JSON));
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        }
        for (Info info : infoList) {
            String jsonInfo = JSON.toJSONString(info);
            request.add(new IndexRequest("info").id(String.valueOf(info.getId())).source(jsonInfo, XContentType.JSON));
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        }
        for (User user : userList) {
            String jsonUser = JSON.toJSONString(user);
            jedis.setex("user:" + user.getUserId(), 86400, jsonUser);
            jedis.setex("user:" + user.getUsername(), 86400, String.valueOf(user.getUserId()));
        }
        jedis.close();
    }

    /**
     * 为了测试方便，每次重启，清空redis
     */
    @Override
    public void destroy() {
        Jedis jedis = jedisPool.getResource();
        jedis.flushAll();
        jedis.close();
    }
}

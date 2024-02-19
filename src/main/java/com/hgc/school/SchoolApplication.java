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
import org.springframework.context.annotation.Bean;
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
public class SchoolApplication  implements InitializingBean, DisposableBean {
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


    @Override
    public void afterPropertiesSet() throws Exception {
        BulkRequest request = new BulkRequest();
        request.timeout("300m");
        Jedis jedis = jedisPool.getResource();
        List<CommentInfo> commentList = commentMapper.selectAll();
        List<Info> infoList = infoMapper.selectAll();
        List<User> userList = userMapper.selectAll();
        for (CommentInfo comment : commentList) {
            String jsonComment = JSON.toJSONString(comment);
            jedis.set("comment:"+ comment.getCommentId(),jsonComment);
            jedis.expire("comment:"+comment.getCommentId(),60*60*24);
            jedis.lpush("comment:pubId:"+comment.getPubId(), String.valueOf(comment.getCommentId()));
            request.add(new IndexRequest("comment").id(String.valueOf(comment.getCommentId())).source(jsonComment, XContentType.JSON));
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        }
        for(Info info : infoList){
            String jsonInfo = JSON.toJSONString(info);
            jedis.set("info:"+info.getId(),jsonInfo);
            jedis.lpush("info:userId:"+info.getUserId(), String.valueOf(info.getId()));
            jedis.expire("info:"+info.getId(),60*60*24);
            request.add(new IndexRequest("info").id(String.valueOf(info.getId())).source(jsonInfo, XContentType.JSON));
            restHighLevelClient.bulk(request, RequestOptions.DEFAULT);
        }
        for (User user : userList) {
            String jsonUser  = JSON.toJSONString(user);
            jedis.set("user:"+user.getUserId(),jsonUser);
            jedis.set("user:"+user.getUsername(), String.valueOf(user.getUserId()));
        }
        jedis.close();
    }

//    @Bean
//    public FilterRegistrationBean filterRegistrationBean(){
//        FilterRegistrationBean<LoginCheckFilter> bean = new FilterRegistrationBean<LoginCheckFilter>();
//        bean.setFilter(new LoginCheckFilter());
//        bean.setOrder(1);
//        return bean;
//    }

    @Override
    public void destroy() throws Exception {
        Jedis jedis = jedisPool.getResource();
        jedis.flushAll();
        jedis.close();
    }
}

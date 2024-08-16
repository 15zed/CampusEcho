package com.hgc.campusechouserservice;

import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;
import com.hgc.campusechouserservice.mapper.UserMapper;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@SpringBootApplication
@EnableRedisHttpSession
@EnableDubbo
@EnableScheduling
@ComponentScan("com.hgc")
@MapperScan("com.hgc.campusechouserservice.mapper")
public class CampusEchoUserServiceApplication implements InitializingBean {
    @Resource
    private UserMapper userMapper;
    @Resource
    private JedisPool jedisPool;


    public static void main(String[] args) {
        SpringApplication.run(CampusEchoUserServiceApplication.class, args);
    }

    @Override
    public void afterPropertiesSet() {
        Random random = new Random();
        //缓存近一个月活跃用户的关注列表和粉丝前10000个
        List<Integer> userIdList = userMapper.selectPart();
        //设置好初始容量，可以防止频繁扩容造成性能消耗
        List<Following> followingList = new ArrayList<>(2000);
        List<Follower> fansList = new ArrayList<>(10000);
        for (Integer userId : userIdList) {
            followingList = userMapper.selectFollowingIds(userId);
            fansList = userMapper.selectFollowerIds(userId, 0L, 10000);
        }
        try(Jedis jedis = jedisPool.getResource()){
            for (Following following : followingList) {
                jedis.zadd("following_"+following.getFromUserId(),following.getUpdateTime(), String.valueOf(following.getToUserId()));
                jedis.expire("following_"+following.getFromUserId(), random.nextInt(30*24*60*60)+1);
            }
            for (Follower follower : fansList) {
                jedis.zadd("follower_"+follower.getToUserId(), follower.getUpdateTime(), String.valueOf(follower.getFromUserId()));
                jedis.expire("follower_"+follower.getToUserId(), random.nextInt(30*24*60*60)+1);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}

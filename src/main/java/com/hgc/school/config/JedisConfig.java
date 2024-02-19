package com.hgc.school.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 *
 */
@Configuration
public class JedisConfig {
    @Bean
    public JedisPool jedisPool(){
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(200);
        poolConfig.setMinIdle(20);
        poolConfig.setMaxWaitMillis(15000);
        return new JedisPool(poolConfig,"localhost",6379);
    }
}

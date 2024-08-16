package com.hgc.campusechopostsservice.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * caffeine缓存配置
 */
@Configuration
public class CacheConfig {

    @Bean
    public Cache<String, String> caffeineCache() {
        return Caffeine.newBuilder()
                .expireAfterAccess(10, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }

}

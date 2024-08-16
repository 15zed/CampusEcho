package com.hgc.campusechouserservice.schedule;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.util.List;

/**
 * 定时扫描redis，获取前缀为follower_的zset，判断其长度是否大于10000，如果大于10000，则删除其前10000个元素
 * 因为zset是有序的，默认按照分数从小到大排序，score是时间戳，所以删除的是最旧的元素
 */
@Component
public class RedisCleanupTask {

    @Resource
    private JedisPool jedisPool;

    // Jedis connection pool initialization
    private Jedis jedis;

    @PostConstruct
    public void init() {
        // Initialize Jedis connection
        this.jedis = jedisPool.getResource();
    }

    @Scheduled(fixedRate = 10000) // Executes every 10 seconds
    public void cleanUpRedis() {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            // Start scanning for keys with the prefix "follower_"
            String cursor = "0";
            do {
                // Create ScanParams and set MATCH pattern
                ScanParams scanParams = new ScanParams().match("follower_*");

                // Scan for keys with the prefix "follower_"
                ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
                cursor = scanResult.getCursor();
                List<String> keys = scanResult.getResult(); // Corrected to List<String>

                for (String key : keys) {
                    // Get the count of members in the zset
                    long count = jedis.zcard(key);

                    if (count > 10000) {
                        // Remove the oldest members from the zset
                        // Remove elements from start=0 to count-10000-1
                        jedis.zremrangeByRank(key, 0, count - 10001);
                    }
                }
            } while (!"0".equals(cursor));
        } finally {
            if (jedis != null) {
                jedis.close();
            }
        }
    }
}


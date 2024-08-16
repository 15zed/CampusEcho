package com.hgc.campusechocountservice.Util;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

/**
 * 冷数据扫描器
 */
@Component
public class RedisColdDataScanner {

    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private RedissonClient redissonClient;

    private static final int SCAN_COUNT = 100;



    public Set<String> getColdDataKeys(long thresholdTime, int fansThreshold) {
        Set<String> coldDataKeys = new HashSet<>();
        String cursor = "0";
        ScanParams scanParams = new ScanParams().count(SCAN_COUNT);

        do {
            Jedis jedis = jedisPool.getResource();
            ScanResult<String> scanResult = jedis.scan(cursor, scanParams);
            cursor = scanResult.getCursor();
            for (String key : scanResult.getResult()) {
                RLock lock = redissonClient.getLock("lock_" + key);
                try{
                    boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
                    if(!locked){
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to acquire lock");
                    }

                    //对于用户相关的key，判断访问时间是否超过thresholdTime阈值且粉丝数小于fansThreshold，如果符合就加入到set中
                    if (key.startsWith("count_user_")) {
                        String lastAccessTimeStr = jedis.hget(key, "lastAccessTime");
                        String fansCountStr = jedis.hget(key, "fans");
                        if (lastAccessTimeStr != null && fansCountStr != null) {
                            long lastAccessTime = Long.parseLong(lastAccessTimeStr);
                            int fansCount = Integer.parseInt(fansCountStr);
                            if (System.currentTimeMillis() - lastAccessTime > thresholdTime && fansCount <= fansThreshold) {
                                coldDataKeys.add(key);
                            }
                        }
                    }
                    //对于帖子相关的key，判断是否超过thresholdTime * 6阈值未访问，如果超过就加入到set中
                    if(key.startsWith("count_info_")){
                        String lastAccessTimeStr = jedis.hget(key, "lastAccessTime");
                        if (lastAccessTimeStr != null) {
                            long lastAccessTime = Long.parseLong(lastAccessTimeStr);
                            if (System.currentTimeMillis() - lastAccessTime > thresholdTime * 6) {
                                coldDataKeys.add(key);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "scan coldDate error");
                }finally {
                    if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                        lock.unlock();
                    }
                }
            }
        } while (!"0".equals(cursor));

        return coldDataKeys;
    }
}


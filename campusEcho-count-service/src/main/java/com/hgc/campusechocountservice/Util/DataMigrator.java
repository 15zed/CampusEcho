package com.hgc.campusechocountservice.Util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.sql.DataSource;

/**
 * 冷数据迁移工具类
 */
@Component
public class DataMigrator {
    @Autowired
    private JedisPool jedisPool;

    @Autowired
    private DataSource dataSource;

    @Autowired
    private RedissonClient redissonClient;

    private Retryer<Boolean> retryer = RetryerBuilder.<Boolean>newBuilder()
            .retryIfExceptionOfType(SQLException.class)
            .withWaitStrategy(WaitStrategies.fixedWait(5, TimeUnit.SECONDS))
            .withStopStrategy(StopStrategies.stopAfterAttempt(5))
            .build();



    public void migrateColdData(Set<String> coldDataKeys) throws SQLException {
        Connection connection = dataSource.getConnection();
        Jedis jedis = jedisPool.getResource();

        String insertUserSQL = "INSERT INTO user_count (userId, follow_count, fans_count, time) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE follow_count = VALUES(follow_count), fans_count = VALUES(fans_count), time = VALUES(time)";
        String insertInfoSQL = "INSERT INTO info_count (infoId, like_count, comment_count, time) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE like_count = VALUES(like_count), comment_count = VALUES(comment_count), time = VALUES(time)";

        for (String key : coldDataKeys) {
            RLock lock = redissonClient.getLock("lock_" + key);
            try{
                boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
                if (!locked) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to acquire lock");
                }
                retryer.call(() -> {
                    if (key.startsWith("count_user_")) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(insertUserSQL)) {
                            String userId = key.substring("count_user_".length());
                            preparedStatement.setInt(1, Integer.parseInt(userId));
                            preparedStatement.setInt(2, Integer.parseInt(jedis.hget(key, "follows")));
                            preparedStatement.setInt(3, Integer.parseInt(jedis.hget(key, "fans")));
                            preparedStatement.setInt(4, Integer.parseInt(jedis.hget(key, "lastAccessTime")));
                            preparedStatement.executeUpdate();
                        }
                        jedis.del(key);// 从Redis中删除冷数据
                    } else if (key.startsWith("count_info_")) {
                        try (PreparedStatement preparedStatement = connection.prepareStatement(insertInfoSQL)) {
                            String infoId = key.substring("count_info_".length());
                            preparedStatement.setInt(1, Integer.parseInt(infoId));
                            preparedStatement.setInt(2, Integer.parseInt(jedis.hget(key, "likes")));
                            preparedStatement.setInt(3, Integer.parseInt(jedis.hget(key, "comments")));
                            preparedStatement.setInt(4, Integer.parseInt(jedis.hget(key, "lastAccessTime")));
                            preparedStatement.executeUpdate();
                        }
                        jedis.del(key);// 从Redis中删除冷数据
                    }
                    return true;
                });
            } catch (InterruptedException | ExecutionException | RetryException e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to data migration");
            }finally {
                if (lock.isLocked() &&  lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }

        }
    }
}


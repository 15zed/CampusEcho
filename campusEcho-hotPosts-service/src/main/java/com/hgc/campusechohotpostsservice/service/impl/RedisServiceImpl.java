package com.hgc.campusechohotpostsservice.service.impl;

import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.config.RedissonConfig;
import com.hgc.campusechocommon.constant.HotPostsConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechohotpostsservice.service.RedisService;
import com.hgc.campusechohotpostsservice.uitl.ZsetKeyUtil;
import com.hgc.campusechointerfaces.service.CountService;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechomodel.entity.Info;
import org.apache.dubbo.config.annotation.DubboReference;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Classname: RedisServiceImpl
 * <p>
 * Version information:
 * <p>
 * User: zed
 * <p>
 * Date: 2024/7/28
 * <p>
 * Copyright notice:
 */
@Service
public class RedisServiceImpl implements RedisService {
    @Autowired
    private RedissonClient redissonClient;
    @Resource
    private JedisPool jedisPool;
    @DubboReference
    private InfoService infoService;
    @DubboReference
    private CountService countService;


    @Override
    public List<Info> getHotPosts(String key, long start, long end) {
        if(key == null || key.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"key is null");
        }
        if(start < 0 || end <= start) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"start or end is error");
        }

        try(Jedis jedis = jedisPool.getResource()){
            Set<String> infoIdList = jedis.zrevrange(key, start, end);
            return infoService.getInfoListByIds(infoIdList);
        }catch (Exception e) {
           throw new BusinessException(ErrorCode.SYSTEM_ERROR,"get hot posts error");
        }
    }

    /**
     * 更新热榜
     * @param infoId 帖子id
     * @return
     */
//    @Override
//    public String updateHotPosts(Integer infoId) {
//        if(infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR,"infoId is null");
//        Long likes = countService.getLikes(infoId);
//        Long comments = countService.getComments(infoId);
//        double hot = likes * 0.4 + comments * 0.6;
//        //更新所有榜单
//        String dayKey = ZsetKeyUtil.getKey(HotPostsConstant.DAY);
//        String hourKey = ZsetKeyUtil.getKey(HotPostsConstant.HOUR);
//        String weekKey = ZsetKeyUtil.getKey(HotPostsConstant.WEEK);
//        try(Jedis jedis = jedisPool.getResource()){
//            //TODO: 更新的时候，判断key是否存在,如果不存在，那么设置一个过期时间，小时榜榜设置1小时过期，日榜设置24小时过期，周榜设置1周过期，如果存在，那么直接更新
//            jedis.zadd(dayKey, hot, String.valueOf(infoId));
//            jedis.zadd(hourKey, hot, String.valueOf(infoId));
//            jedis.zadd(weekKey, hot, String.valueOf(infoId));
//            return "ok";
//        }catch (Exception e) {
//            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"update hot posts error");
//        }
//    }

    @Override
    public String updateHotPosts(Integer infoId) {
        if (infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "infoId is null");
        Long likes = countService.getLikes(infoId);
        Long comments = countService.getComments(infoId);
        double hot = likes * 0.4 + comments * 0.6;

        // 更新所有榜单
        String dayKey = ZsetKeyUtil.getKey(HotPostsConstant.DAY);
        String hourKey = ZsetKeyUtil.getKey(HotPostsConstant.HOUR);
        String weekKey = ZsetKeyUtil.getKey(HotPostsConstant.WEEK);

        RLock lock = redissonClient.getLock("lock:" + infoId);

        try (Jedis jedis = jedisPool.getResource()) {
            // Acquire the lock with a lease time of 30 seconds
            boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to acquire lock");
            }

            try {
                updateZsetWithExpiry(jedis, dayKey, hot, infoId, 24 * 60 * 60);
                updateZsetWithExpiry(jedis, hourKey, hot, infoId, 60 * 60);
                updateZsetWithExpiry(jedis, weekKey, hot, infoId, 7 * 24 * 60 * 60);

                return "ok";
            } finally {
                // Ensure the lock is unlocked
                if (lock.isLocked() &&  lock.isHeldByCurrentThread()) {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "update hot posts error");
        }
    }

    @Override
    public String deleteHotPosts(Integer infoId) {
        if (infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR, "infoId is null");
        String dayKey = ZsetKeyUtil.getKey(HotPostsConstant.DAY);
        String hourKey = ZsetKeyUtil.getKey(HotPostsConstant.HOUR);
        String weekKey = ZsetKeyUtil.getKey(HotPostsConstant.WEEK);
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zrem(dayKey, String.valueOf(infoId));
            jedis.zrem(hourKey, String.valueOf(infoId));
            jedis.zrem(weekKey, String.valueOf(infoId));
            return "ok";
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "delete hot posts error");
        }
    }

    private void updateZsetWithExpiry(Jedis jedis, String key, double score, Integer infoId, int expireTime) {
        boolean keyExisted = jedis.exists(key);
        jedis.zadd(key, score, String.valueOf(infoId));
        if (!keyExisted) {
            jedis.expire(key, expireTime);
        }
    }

}

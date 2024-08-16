package com.hgc.campusechopostsservice.service.impl;

import com.alibaba.fastjson.JSON;

import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.constant.OperationResultConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechopostsservice.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.util.concurrent.Future;

/**
 *
 */
@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Resource
    private JedisPool jedisPool;


    @Async("threadPoolExecutor")
    public Future<Integer> delete(Integer id) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("info_" + id);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        } catch (Exception e) {
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }

    @Override
    public boolean setUniqueId(String uniqueId) {
        try (Jedis jedis = jedisPool.getResource()) {
            Long result = jedis.setnx(uniqueId, "registered");
            if (result == 1) {
                jedis.expire(uniqueId, 20);
                return true;
            } else {
                return false; //  表示写入失败
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"Failed to set uniqueId in Redis");
        }
    }

    @Override
    public String getInfoById(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.get("info_" + infoId);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to get info from Redis");
        }
    }

    @Override
    public void setInfoById(Integer infoId, String jsonString) {
        try(Jedis jedis = jedisPool.getResource()) {
            jedis.set("info_" + infoId, jsonString);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to set info in Redis");
        }
    }
}

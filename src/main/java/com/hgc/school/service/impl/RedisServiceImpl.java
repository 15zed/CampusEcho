package com.hgc.school.service.impl;

import com.alibaba.fastjson.JSON;
import com.hgc.school.Constants.OperationResultConstant;
import com.hgc.school.service.InfoService;
import com.hgc.school.service.RedisService;
import com.hgc.school.service.UserService;
import com.hgc.school.utils.ESIndexUtil;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.delete.DeleteRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private InfoService infoService;

    @Async("threadPoolExecutor")
    @Override
    public <T> Future<Integer> add(T obj) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (obj instanceof Info) {
                Info info = (Info) obj;
                String infoKey = "info:" + info.getId();
                String userIdKey = "info:userId:" + info.getUserId();

                jedis.set(infoKey, JSON.toJSONString(info));
                jedis.expire(infoKey, 60 * 60 * 24);
                jedis.lpush(userIdKey, String.valueOf(info.getId()));
                jedis.expire(userIdKey, 60 * 60 * 24);
                return new AsyncResult<>(OperationResultConstant.SUCCESS);
            } else if (obj instanceof CommentInfo) {
                CommentInfo comment = (CommentInfo) obj;
                String commentKey = "comment:" + comment.getCommentId();
                String pubIdKey = "comment:pubId:" + comment.getPubId();
                String pubInfoKey = "info:" + comment.getPubId();

                jedis.set(commentKey, JSON.toJSONString(comment));
                jedis.expire(commentKey, 60 * 60 * 24);
                jedis.lpush(pubIdKey, String.valueOf(comment.getCommentId()));
                jedis.expire(pubIdKey, 60 * 60 * 24);
                jedis.set(pubInfoKey, JSON.toJSONString(infoService.selectById(comment.getPubId())));

                return new AsyncResult<>(OperationResultConstant.SUCCESS);
            } else {
                throw new IllegalArgumentException("参数错误：" + obj);
            }
        } catch (Exception e) {
            log.error("操作失败：" + e.getMessage());
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }

    }
    @Async("threadPoolExecutor")
    public Future<Integer> delete(Integer id) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.del("user:" + id);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        } catch (Exception e) {
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }
}

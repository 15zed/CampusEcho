package com.hgc.campusechocommentservice.service.impl;


import com.alibaba.fastjson.JSON;
import com.hgc.campusechocommentservice.service.RedisService;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.constant.OperationResultConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechomodel.entity.CommentInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import javax.annotation.Resource;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Resource
    private JedisPool jedisPool;
    @DubboReference
    private InfoService infoService;

//    @Async("threadPoolExecutor")
//    @Override
//    public <T> Future<Integer> add(CommentInfo comment) {
//        try (Jedis jedis = jedisPool.getResource()) {
//            long time = comment.getTime().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
//            if(!jedis.exists("comment_list_"+comment.getCommentId())){
//                //将评论id存入zset对象
//                jedis.zadd("comment_list_"+comment.getCommentId(),time, String.valueOf(comment.getCommentId()));
//                jedis.expire("comment_list_"+comment.getCommentId(), 5*24*60*60);
//                //将评论数据存入string对象
//                jedis.setex("comment_"+comment.getCommentId(),5*24*60*60, JSON.toJSONString(comment));
//                return new AsyncResult<>(OperationResultConstant.SUCCESS);
//            }else {
//                //TODO: lua脚本判断key=“comment_”+comment.getCommentId()的zset对象的大小是否小于等于100，条件成立则执行和上面if判断中一样的操作，否则不执行
//                return new AsyncResult<>(OperationResultConstant.SUCCESS);
//            }
//        }
//        catch (Exception e) {
//            log.error("操作失败：" + e.getMessage());
//            return new AsyncResult<>(OperationResultConstant.FAILURE);
//        }
//    }

    @Async("threadPoolExecutor")
    @Override
    public <T> Future<Integer> add(CommentInfo comment) {
        String commentId = String.valueOf(comment.getCommentId());
        String zsetKey = "comment_list_" + commentId;
        String stringKey = "comment_" + commentId;
        long expirationTime = 5 * 24 * 60 * 60; // 5 days in seconds

        try (Jedis jedis = jedisPool.getResource()) {
            String script =
                    "local zsetKey = KEYS[1] " +
                            "local stringKey = KEYS[2] " +
                            "local maxSize = 100 " +
                            "local time = ARGV[1] " +
                            "local expirationTime = ARGV[2] " +
                            "if redis.call('exists', zsetKey) == 1 then " +
                            "    local size = redis.call('zcard', zsetKey) " +
                            "    if size < maxSize then " +
                            "        redis.call('zadd', zsetKey, time, stringKey) " +
                            "        redis.call('expire', zsetKey, expirationTime) " +
                            "        redis.call('setex', stringKey, expirationTime, ARGV[3]) " +
                            "        return 1 " +
                            "    else " +
                            "        return 0 " +
                            "    end " +
                            "else " +
                            "    redis.call('zadd', zsetKey, time, stringKey) " +
                            "    redis.call('expire', zsetKey, expirationTime) " +
                            "    redis.call('setex', stringKey, expirationTime, ARGV[3]) " +
                            "    return 1 " +
                            "end";

            long time = comment.getTime().atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
            String commentJson = JSON.toJSONString(comment);

            Object result = jedis.eval(script,
                    Arrays.asList(zsetKey, stringKey),
                    Arrays.asList(String.valueOf(time),
                            String.valueOf(expirationTime),
                            commentJson));

            int operationResult = (Long) result == 1 ? OperationResultConstant.SUCCESS : OperationResultConstant.FAILURE;
            return new AsyncResult<>(operationResult);
        } catch (Exception e) {
            log.error("操作失败：" + e.getMessage());
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }


    @Async("threadPoolExecutor")
    public Future<Integer> delete(Integer commentId) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zrem("comment_list_"+commentId, String.valueOf(commentId));
            jedis.del("comment_"+commentId);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        }
        catch (Exception e) {
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
            }
            else {
                return false; //  表示写入失败
            }
        }
        catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to set uniqueId in Redis");
        }
    }

    @Override
    public List<Integer> getIds(Integer infoId,long start, long end) {
        if(infoId == null)  throw new BusinessException(ErrorCode.NULL_ERROR, "infoId is null");
        try (Jedis jedis = jedisPool.getResource()) {
            if(jedis.exists("comment_list_" + infoId)){
                return jedis.zrange("comment_list_" + infoId, start, end).stream().map(Integer::parseInt).collect(java.util.stream.Collectors.toList());
            }else {
                return new ArrayList<>();
            }
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to get commentIds in Redis");
        }
    }

    @Override
    public List<CommentInfo> getComments(List<Integer> commentIdList) {
        List<CommentInfo> comments = new ArrayList<>();
        try (Jedis jedis = jedisPool.getResource()) {
            for(Integer commentId : commentIdList){
                comments.add(JSON.parseObject(jedis.get("comment_"+commentId), CommentInfo.class));
            }
            return comments;
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to get comments in Redis");
        }
    }


}

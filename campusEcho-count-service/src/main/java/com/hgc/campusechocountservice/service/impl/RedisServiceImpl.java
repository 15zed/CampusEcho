package com.hgc.campusechocountservice.service.impl;

import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechocountservice.service.RedisService;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;

/**
 * 这里的增加计数方法和减少计数方法，都只有一条命令需要执行，不需要使用事务或者Lua脚本来保证原子性
 * 如果需要确保多条命令作为一个不可分割的整体执行，才需要使用事务或者Lua脚本
 */
@Service
public class RedisServiceImpl implements RedisService {
    @Resource
    JedisPool jedisPool;


    @Override
    public Long getLikes(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()){
            return Long.parseLong(jedis.hget("count_info_"+infoId,"likes"));
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取点赞数失败");
        }
    }

    @Override
    public Long getComments(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()){
            return Long.parseLong(jedis.hget("count_info_"+infoId,"comments"));
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取评论数失败");
        }
    }

    @Override
    public Long getFollows(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()){
            return Long.parseLong(jedis.hget("count_user_"+userId,"follows"));
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取关注数失败");
        }
    }

    @Override
    public Long getFans(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()){
            return Long.parseLong(jedis.hget("count_user_"+userId,"fans"));
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"获取粉丝数失败");
        }
    }

    @Override
    public boolean existInfoKey(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.exists("count_info_" + infoId);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"判断key是否存在失败");
        }
    }

    @Override
    public Long addLikes(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hincrBy("count_info_" + infoId, "likes", 1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"增加点赞数失败");
        }
    }

    @Override
    public Long addComments(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hincrBy("count_info_" + infoId, "comments", 1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"增加评论数失败");
        }
    }

    @Override
    public Long addFollows(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()){
            // hincrBy是线程安全的，不需要使用事务或者Lua脚本
            return jedis.hincrBy("count_user_" + userId, "follows", 1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"增加关注数失败");
        }
    }

    @Override
    public Long addFans(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()){
            // hincrBy是线程安全的，不需要使用事务或者Lua脚本
            return jedis.hincrBy("count_user_" + userId, "fans", 1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"增加粉丝数失败");
        }
    }

    @Override
    public Long reduceLikes(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()){
            // hincrBy是线程安全的，不需要使用事务或者Lua脚本
            return jedis.hincrBy("count_info_" + infoId, "likes", -1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"减少点赞数失败");
        }
    }

    @Override
    public void saveFans(Integer userId, Long fans) {
        try(Jedis jedis = jedisPool.getResource()){
            jedis.hset("count_user_" + userId, "fans", String.valueOf(fans));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存粉丝数失败");
        }
    }

    @Override
    public boolean existUserKey(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.exists("count_user_" + userId);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"判断key是否存在失败");
        }
    }

    @Override
    public void saveFollows(Integer userId, Long follows) {
        try(Jedis jedis = jedisPool.getResource()){
            jedis.hset("count_user_" + userId, "follows", String.valueOf(follows));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存关注数失败");
        }
    }

    @Override
    public void saveLikes(Integer infoId, Long likes) {
        try(Jedis jedis = jedisPool.getResource()){
            jedis.hset("count_info_" + infoId, "likes", String.valueOf(likes));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存点赞数失败");
        }
    }

    @Override
    public void saveComments(Integer infoId, Long comments) {
        try(Jedis jedis = jedisPool.getResource()){
            jedis.hset("count_info_" + infoId, "comments", String.valueOf(comments));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存评论数失败");
        }
    }

    @Override
    public Long reduceComments(Integer infoId) {
        try(Jedis jedis = jedisPool.getResource()){
            // hincrBy是线程安全的，不需要使用事务或者Lua脚本
            return jedis.hincrBy("count_info_" + infoId, "comments", -1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"减少评论数失败");
        }
    }

    @Override
    public Long reduceFollows(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hincrBy("count_user_" + userId, "follows", -1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"减少关注数失败");
        }
    }

    @Override
    public Long reduceFans(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()){
            return jedis.hincrBy("count_user_" + userId, "fans", -1);
        }catch (Exception e){
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"减少粉丝数失败");
        }
    }

    @Override
    public void updateInfoTime(Integer infoId, long currentTimeMillis) {
        if(infoId != null){
            try(Jedis jedis = jedisPool.getResource()){
                jedis.hset("count_info_" + infoId, "lastAccessTime", String.valueOf(currentTimeMillis));
            }catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新信息时间失败");
            }
        }
    }

    @Override
    public void updateUserTime(Integer userId, long currentTimeMillis) {
         if(userId != null){
             try(Jedis jedis = jedisPool.getResource()){
                 jedis.hset("count_user_" + userId, "lastAccessTime", String.valueOf(currentTimeMillis));
             }catch (Exception e) {
                 throw new BusinessException(ErrorCode.SYSTEM_ERROR, "更新用户时间失败");
             }
         }
    }

    @Override
    public String delKeyByInfoId(Integer infoId) {
        String result = null;
        if(infoId != null){
            try(Jedis jedis = jedisPool.getResource()){
                jedis.del("count_info_" + infoId);
                result = "删除成功";
            }catch (Exception e) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "删除信息失败");
            }
        }
        return result;
    }
}

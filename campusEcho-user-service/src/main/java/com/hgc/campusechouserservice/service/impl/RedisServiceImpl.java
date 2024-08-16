package com.hgc.campusechouserservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.constant.OperationResultConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.InfoService;

import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechouserservice.service.RedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.annotation.Resource;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 *
 */
@Service
@Slf4j
public class RedisServiceImpl implements RedisService {
    @Resource
    private JedisPool jedisPool;
    @Resource
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
    public Set<String> selectFollows(Integer userId) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrange("following_"+userId, 0, -1);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"Failed to select follows in Redis");
        }
    }

    @Override
    public void addFollows(Integer userId, List<Following> followList) {
        try(Jedis jedis = jedisPool.getResource()) {
           followList.forEach(following -> {
               jedis.zadd("following_"+userId, following.getUpdateTime(), String.valueOf(following.getToUserId()));
           });
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to add follows in Redis");
        }
    }

    @Override
    public Set<String> selectFans(Integer userId,long start,long end) {
        try (Jedis jedis = jedisPool.getResource()) {
            return jedis.zrevrange("follower_"+userId, start, end);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"Failed to select fans in Redis");
        }
    }

    @Override
    public void addFans(Integer userId, List<Follower> followerList) {
        try (Jedis jedis = jedisPool.getResource()) {
            followerList.forEach(follower -> {
                jedis.zadd("follower_" + userId, follower.getUpdateTime(), String.valueOf(follower.getFromUserId()));
            });
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to add fans in Redis");
        }
    }

    @Override
    public void deleteFollow(Integer userId) {
        try(Jedis jedis = jedisPool.getResource()) {
            jedis.del("following_"+userId);
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to delete follow in Redis");
        }
    }

    @Override
    public void insertFollowing(Following following) {
        try(Jedis jedis = jedisPool.getResource()) {
            jedis.zadd("following_"+following.getFromUserId(), following.getUpdateTime(), String.valueOf(following.getToUserId()));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to insert follow in Redis");
        }
    }

    @Override
    public void insertFollower(Follower follower) {
        try (Jedis jedis = jedisPool.getResource()) {
            // 如果要执行注释的逻辑需要使用事务或者Lua脚本保证并发安全，换一种方式，直接使用定时任务定期删除多余的成员
//            if(jedis.exists("follower_"+follower.getToUserId()) && jedis.zcard("follower_"+follower.getToUserId()) > 10000){
//                // 移除分数最低的成员，也就是最晚关注的那个成员
//                jedis.zremrangeByRank("follower_"+follower.getToUserId(),0,0);
//            }
            jedis.zadd("follower_" + follower.getToUserId(), follower.getUpdateTime(), String.valueOf(follower.getFromUserId()));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to insert follower in Redis");
        }
    }

    @Override
    public void updateFollowing(Following following) {
        try(Jedis jedis = jedisPool.getResource()) {
            if(following.getType() == 2) {
                // 如果是A取消关注B，则把B从A的关注列表缓存中删除，否则添加
                jedis.zrem("following_" + following.getFromUserId(), String.valueOf(following.getToUserId()));
            }else {
                jedis.zadd("following_" + following.getFromUserId(), following.getUpdateTime(), String.valueOf(following.getToUserId()));
            }
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to update follow in Redis");
        }
    }

    @Override
    public void updateFollower(Follower follower) {
        try(Jedis jedis = jedisPool.getResource()) {
            if(follower.getType() == 2) {
                // 如果是A取消关注B，则把A从B的粉丝列表缓存中删除，否则添加
                jedis.zrem("follower_" + follower.getToUserId(), String.valueOf(follower.getFromUserId()));
            }else {
                jedis.zadd("follower_" + follower.getToUserId(), follower.getUpdateTime(), String.valueOf(follower.getFromUserId()));
            }
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to update follower in Redis");
        }
    }

    @Override
    public Long selectIfFollow(Integer userId, Integer userId1) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.zrank("following_" + userId, String.valueOf(userId1));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to select follow in Redis");
        }
    }

    @Override
    public Long selectIfFollower(Integer userId, Integer userId1) {
        try(Jedis jedis = jedisPool.getResource()) {
            return jedis.zrank("follower_" + userId, String.valueOf(userId1));
        }catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Failed to select follower in Redis");
        }
    }

}

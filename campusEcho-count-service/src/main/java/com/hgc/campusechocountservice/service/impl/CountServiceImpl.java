package com.hgc.campusechocountservice.service.impl;


import com.hgc.campusechocountservice.mapper.CountMapper;
import com.hgc.campusechocountservice.mapper.LikesMapper;
import com.hgc.campusechocountservice.service.RedisService;
import com.hgc.campusechointerfaces.service.CountService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 *
 */
@Service
@DubboService
public class CountServiceImpl implements CountService {
    @Resource
    RedisService redisService;

    @Autowired
    private CountMapper countMapper;


    @Override
    public Long getLikes(Integer infoId) {
        Long likes = redisService.getLikes(infoId);
        if(likes == null){
            //去数据库查
            likes = countMapper.getLikes(infoId);
            //存入redis
            redisService.saveLikes(infoId,likes);
            //更新访问时间
            updateInfoTime(infoId);
            //删除MySQL
            countMapper.deleteInfoRecord(infoId);
        }
        return likes;
    }

    @Override
    public Long getComments(Integer infoId) {
        Long comments = redisService.getComments(infoId);
        if(comments == null){
            //去数据库查
            comments = countMapper.getComments(infoId);
            //存入redis
            redisService.saveComments(infoId,comments);
            //更新访问时间
            updateInfoTime(infoId);
            //删除MySQL
            countMapper.deleteInfoRecord(infoId);
        }
        return comments;
    }

    @Override
    public Long getFollows(Integer userId) {
        Long follows = redisService.getFollows(userId);
        if(follows == null){
            //去数据库查
            follows = countMapper.getFollows(userId);
            //存入redis
            redisService.saveFollows(userId,follows);
            //更新访问时间
            updateUserTime(userId);
            //删除MySQL
            countMapper.deleteUserRecord(userId);
        }
        updateUserTime(userId);
        return follows;
    }

    @Override
    public Long getFans(Integer userId) {
        Long fans = redisService.getFans(userId);
        if(fans == null){
            //去数据库查
            fans = countMapper.getFans(userId);
            //存入redis
            redisService.saveFans(userId,fans);
            //更新访问时间
            updateUserTime(userId);
            //删除MySQL
            countMapper.deleteUserRecord(userId);
        }
        return fans;
    }

    @Override
    public Long addLikes(Integer infoId) {
        Long likes;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existInfoKey(infoId)){
            likes = redisService.addLikes(infoId);
            updateInfoTime(infoId);
            return likes;
        }else if(!countMapper.existInfo(infoId)){
            //如果redis中不存在，MySQL中也不存在，说明是第一次插入，直接插入redis
            likes = redisService.addLikes(infoId);
            updateInfoTime(infoId);
            return likes;
        }else{
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlLikes = countMapper.getLikes(infoId);
            likes = mysqlLikes+1;
            countMapper.deleteInfoRecord(infoId);
            redisService.saveLikes(infoId,likes);
            updateInfoTime(infoId);
        }
        return likes;
    }

    @Override
    public Long addComments(Integer infoId) {
        Long comments;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existInfoKey(infoId)){
            comments = redisService.addComments(infoId);
            updateInfoTime(infoId);
            return comments;
        }else if(!countMapper.existInfo(infoId)){
            //如果redis中不存在，MySQL中也不存在，说明是第一次插入，直接插入redis
            comments = redisService.addComments(infoId);
            updateInfoTime(infoId);
            return comments;
        }else{
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlComments = countMapper.getComments(infoId);
            comments = mysqlComments+1;
            countMapper.deleteInfoRecord(infoId);
            redisService.saveComments(infoId,comments);
            updateInfoTime(infoId);
        }
        return comments;
    }

    @Override
    public Long addFollows(Integer userId) {
        Long follows;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existUserKey(userId)){
            follows = redisService.addFollows(userId);
            updateUserTime(userId);
            return follows;
        }else if(!countMapper.existUser(userId)){
            //如果redis中不存在，MySQL中也不存在，说明是第一次插入，直接插入redis
            follows = redisService.addFollows(userId);
            updateUserTime(userId);
            return follows;
        }else{
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlFollows = countMapper.getFollows(userId);
            follows = mysqlFollows+1;
            countMapper.deleteUserRecord(userId);
            redisService.saveFollows(userId,follows);
            updateUserTime(userId);
        }
        return follows;
    }

    @Override
    public Long addFans(Integer userId) {
        Long fans;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existUserKey(userId)){
            fans = redisService.addFans(userId);
            updateUserTime(userId);
            return fans;
        }else if(!countMapper.existUser(userId)){
            //如果redis中不存在，MySQL中也不存在，说明是第一次插入，直接插入redis
            fans = redisService.addFans(userId);
            updateUserTime(userId);
            return fans;
        }else{
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlFans = countMapper.getFans(userId);
            fans = mysqlFans+1;
            countMapper.deleteUserRecord(userId);
            redisService.saveFans(userId,fans);
            updateUserTime(userId);
        }
        return fans;
    }

    @Override
    public Long reduceLikes(Integer infoId, Integer userId) {
        Long latestLikes = null;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existInfoKey(infoId)){
            latestLikes = redisService.reduceLikes(infoId);
            updateInfoTime(infoId);
        }else if(countMapper.existInfo(infoId)){
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlLikes = countMapper.getLikes(infoId);
            latestLikes = mysqlLikes-1;
            countMapper.deleteInfoRecord(infoId);
            redisService.saveLikes(infoId,latestLikes);
            updateInfoTime(infoId);
        }
        return latestLikes;
    }

    @Override
    public Long reduceComments(Integer infoId) {
        Long latestComments = null;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existInfoKey(infoId)){
            latestComments = redisService.reduceComments(infoId);
            updateInfoTime(infoId);
            return latestComments;
        }else if(countMapper.existInfo(infoId)){
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlComments = countMapper.getComments(infoId);
            latestComments = mysqlComments-1;
            countMapper.deleteInfoRecord(infoId);
            redisService.saveComments(infoId,latestComments);
            updateInfoTime(infoId);
        }
        return latestComments;
    }

    @Override
    public Long reduceFollows(Integer userId) {
        Long latestFollows = null;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existUserKey(userId)){
            latestFollows = redisService.reduceFollows(userId);
            updateUserTime(userId);
        }else if(countMapper.existUser(userId)){
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlFollows = countMapper.getFollows(userId);
            latestFollows = mysqlFollows-1;
            countMapper.deleteUserRecord(userId);
            redisService.saveFollows(userId,latestFollows);
            updateUserTime(userId);
        }
        return latestFollows;
    }

    @Override
    public Long reduceFans(Integer userId) {
        Long latestFans = null;
        //如果redis中存在，直接更新，返回结果
        if(redisService.existUserKey(userId)){
            latestFans = redisService.reduceFans(userId);
            updateUserTime(userId);
        }else if(countMapper.existUser(userId)){
            //如果redis中不存在，MySQL中存在，更新MySQL，写回redis，最后删除MySQL
            Long mysqlFans = countMapper.getFans(userId);
            latestFans = mysqlFans-1;
            countMapper.deleteUserRecord(userId);
            redisService.saveFans(userId,latestFans);
            updateUserTime(userId);
        }
        return latestFans;
    }

    public void updateInfoTime(Integer infoId){
        long currentTimeMillis = System.currentTimeMillis();
        redisService.updateInfoTime(infoId,currentTimeMillis);
    }

    public void updateUserTime(Integer userId){
        long currentTimeMillis = System.currentTimeMillis();
        redisService.updateUserTime(userId,currentTimeMillis);
    }

    @Override
    public String deleteCountByInfoId(Integer infoId) {
        return redisService.delKeyByInfoId(infoId);
    }
}

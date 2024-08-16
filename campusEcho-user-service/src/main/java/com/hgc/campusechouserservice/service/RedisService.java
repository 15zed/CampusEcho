package com.hgc.campusechouserservice.service;


import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

/**
 *
 */
public interface RedisService {

    <T> Future<Integer> add(T obj);

    Future<Integer> delete(Integer id);

     boolean setUniqueId(String uniqueId);

    Set<String> selectFollows(Integer userId);

    void addFollows(Integer userId, List<Following> followIdList);

    Set<String> selectFans(Integer userId,long start,long end);

    void addFans(Integer userId, List<Follower> followerList);

    void deleteFollow(Integer userId);

    void insertFollowing(Following following);

    void insertFollower(Follower follower);

    void updateFollowing(Following following);

    void updateFollower(Follower follower);

    Long selectIfFollow(Integer userId, Integer userId1);

    Long selectIfFollower(Integer userId, Integer userId1);



}

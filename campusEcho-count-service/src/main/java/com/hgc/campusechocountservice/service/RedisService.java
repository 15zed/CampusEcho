package com.hgc.campusechocountservice.service;



/**
 *
 */
public interface RedisService {

    Long getLikes(Integer infoId);

    Long getComments(Integer infoId);

    Long getFollows(Integer userId);

    Long getFans(Integer userId);

    Long addLikes(Integer infoId);

    Long addComments(Integer infoId);

    Long addFollows(Integer userId);

    Long addFans(Integer userId);

    Long reduceLikes(Integer infoId);

    Long reduceComments(Integer infoId);

    Long reduceFollows(Integer userId);

    Long reduceFans(Integer userId);

    void updateInfoTime(Integer infoId, long currentTimeMillis);

    void updateUserTime(Integer userId, long currentTimeMillis);

    String delKeyByInfoId(Integer infoId);

    void saveComments(Integer infoId, Long comments);

    void saveLikes(Integer infoId, Long likes);

    void saveFollows(Integer userId, Long follows);

    void saveFans(Integer userId, Long fans);

    boolean existInfoKey(Integer infoId);

    boolean existUserKey(Integer userId);
}

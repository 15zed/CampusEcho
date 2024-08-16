package com.hgc.campusechointerfaces.service;

/**
 *
 */
public interface CountService {
    Long getLikes(Integer infoId);


    Long getComments(Integer infoId);

    Long getFollows(Integer userId);

    Long getFans(Integer userId);

    Long addLikes(Integer infoId);

    Long addComments(Integer infoId);

    Long addFollows(Integer userId);

    Long addFans(Integer userId);

    Long reduceLikes(Integer infoId,Integer userId);

    Long reduceComments(Integer infoId);

    Long reduceFollows(Integer userId);

    Long reduceFans(Integer userId);


    String deleteCountByInfoId(Integer infoId);
}

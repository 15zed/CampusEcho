package com.hgc.campusechocountservice.mapper;

import org.apache.ibatis.annotations.Mapper;

/**
 *
 */
@Mapper
public interface CountMapper {

    Long getComments(Integer infoId);

    Long getLikes(Integer infoId);


    void deleteInfoRecord(Integer infoId);


    Long getFollows(Integer userId);

    void deleteUserRecord(Integer userId);

    Long getFans(Integer userId);

    boolean existInfo(Integer infoId);

    boolean existUser(Integer userId);
}

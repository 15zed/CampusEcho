package com.hgc.campusechosearchandrecommendservice.mapper;

import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 *
 */
@Mapper
public interface LikesMapper {

    List<Integer> getUserLikeList(Integer userId);
}

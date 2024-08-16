package com.hgc.campusechocountservice.mapper;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 *
 */
@Mapper
public interface LikesMapper {

    List<Integer> getInfoIdList();
    List<Integer> getInfoIdListByUserId(Integer userId);

    void insertLikes(Integer infoId, Integer userId);

    void deleteLikes(Integer infoId, Integer userId);

    void deleteLikesByInfoId(Integer infoId);


}

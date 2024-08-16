package com.hgc.campusechointerfaces.service;

import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.Info;

import java.util.List;

/**
 *
 */
public interface HotPostsService {

    List<Info> getHotPosts(String key, long start, long end);

    String updateHotPosts(Integer infoId);

    String deleteHotPosts(Integer infoId);
}

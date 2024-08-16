package com.hgc.campusechohotpostsservice.service;

import com.hgc.campusechomodel.entity.Info;

import java.util.List;

/**
 *
 */
public interface RedisService {

    List<Info> getHotPosts(String key, long start, long end);

    String updateHotPosts(Integer infoId);

    String deleteHotPosts(Integer infoId);
}

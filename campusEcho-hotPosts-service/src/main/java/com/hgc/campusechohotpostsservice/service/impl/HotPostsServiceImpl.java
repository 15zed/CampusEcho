package com.hgc.campusechohotpostsservice.service.impl;

import com.hgc.campusechohotpostsservice.service.RedisService;
import com.hgc.campusechointerfaces.service.HotPostsService;
import com.hgc.campusechomodel.entity.Info;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */
@Service
@DubboService
public class HotPostsServiceImpl implements HotPostsService {
    @Autowired
    private RedisService redisService;

    @Override
    public List<Info> getHotPosts(String key, long start, long end) {
        return redisService.getHotPosts(key, start, end);
    }

    @Override
    public String updateHotPosts(Integer infoId) {
        return redisService.updateHotPosts(infoId);
    }

    @Override
    public String deleteHotPosts(Integer infoId) {
        return redisService.deleteHotPosts(infoId);
    }
}

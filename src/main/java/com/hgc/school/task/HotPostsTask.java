package com.hgc.school.task;

import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.utils.CalculateHotPostsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 *
 */
@Component
public class HotPostsTask {
    @Autowired
    private CalculateHotPostsUtil calculateHotPostsUtil;

    private List<InfoWithCommentsDTO> hotPostsCache; // 用于缓存计算结果

    // 每隔一小时执行一次
    @Scheduled(fixedRate = 3600000)
    public void calculateAndCacheHotPosts() {
        // 计算并缓存热点帖子数据
        hotPostsCache = calculateHotPostsUtil.calculate();
    }

    public List<InfoWithCommentsDTO> getHotPosts() {
        // 直接返回缓存的结果
        return hotPostsCache;
    }


}

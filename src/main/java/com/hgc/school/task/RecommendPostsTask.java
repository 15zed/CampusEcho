package com.hgc.school.task;

import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.utils.CalculateRecommendPostsUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 *
 */
@Component
public class RecommendPostsTask {
    @Autowired
    private CalculateRecommendPostsUtil recommendPostsUtil;

    private Map<Integer,List<InfoWithCommentsDTO>> recommendPosts;// 用于缓存计算结果

    // 每填晚上3:00执行一次
//    @Scheduled(cron = "0 0 3 * * *")
    @Scheduled(fixedRate = 3600000)
    public void calculateAndCacheRecommendPosts() {
        // 计算并缓存推荐帖子数据
        recommendPosts = recommendPostsUtil.calculate();
    }

    public List<InfoWithCommentsDTO> getRecommendPosts(Integer userId) {
        if(userId == null || userId.equals(0)) throw new RuntimeException("出错");
        return recommendPosts.get(userId);
    }
}

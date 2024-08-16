package com.hgc.campusechosearchandrecommendservice.task;

import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechosearchandrecommendservice.utils.CalculateRecommendPostsUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import javax.annotation.Resource;
import java.util.List;
import java.util.Map;


/**
 * 推荐帖子任务
 */
@Component
public class RecommendPostsTask {
    @Resource
    private CalculateRecommendPostsUtil recommendPostsUtil;// 推荐帖子计算工具

    private Map<Integer, List<InfoWithCommentsDTO>> recommendPosts;// 用于缓存计算结果

    // 每天晚上3:00执行一次
    @Scheduled(cron = "0 0 3 * * *")
    public void calculateAndCacheRecommendPosts() {
        // 计算并缓存推荐帖子数据
        recommendPosts = recommendPostsUtil.calculate();
    }

    /**
     * 获取推荐帖子
     * @param userId 用户id
     * @return 推荐帖子
     */
    public List<InfoWithCommentsDTO> getRecommendPosts(Integer userId) {
        if(userId == null || userId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return recommendPosts.get(userId);
    }
}

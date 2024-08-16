package com.hgc.campusechosearchandrecommendservice.controller;


import com.alibaba.fastjson.JSON;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;

import com.hgc.campusechointerfaces.service.ESService;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.User;

import com.hgc.campusechosearchandrecommendservice.task.RecommendPostsTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.util.List;


/**
 *
 */
@RestController
@RequestMapping("/searchandrecommend")
public class SearchandRecommendController {
    @Resource
    private RecommendPostsTask recommendPostsTask;
    @Resource
    private ESService esService;


    /**
     * ES查询帖子
     *
     * @param keyword
     * @return
     */
    @GetMapping("/search/{keyword}")
    public String searchPost(@PathVariable String keyword) {
        if (keyword == null || keyword.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<InfoWithCommentsDTO> dtoList = esService.search(keyword);
        return JSON.toJSONString(dtoList);
    }

    /**
     * 推荐帖子
     *
     * @return
     */
    @GetMapping("/recommendPosts")
    public String getRecommendPosts(HttpSession session) {
        String jsonUser = (String) session.getAttribute("user");
        if (jsonUser == null || jsonUser.isEmpty()) throw new BusinessException(ErrorCode.NULL_ERROR, "用户信息不存在");
        User user = JSON.parseObject(jsonUser, User.class);
        //调用定时任务返回推荐帖子数据，转成json给前端
        List<InfoWithCommentsDTO> recommendPosts = recommendPostsTask.getRecommendPosts(user.getUserId());
        return JSON.toJSONString(recommendPosts);
    }
}

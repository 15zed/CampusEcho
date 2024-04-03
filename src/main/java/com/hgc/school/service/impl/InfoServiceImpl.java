package com.hgc.school.service.impl;

import com.alibaba.fastjson.JSON;
import com.hgc.school.commons.ErrorCode;
import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.exception.BusinessException;
import com.hgc.school.mapper.InfoMapper;
import com.hgc.school.service.CommentService;
import com.hgc.school.service.ESService;
import com.hgc.school.service.InfoService;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
@Service
public class InfoServiceImpl implements InfoService {
    @Autowired
    private InfoMapper infoMapper;
    @Autowired
    CommentService commentService;


    @Override
    public List<Info> getData() {
        return infoMapper.selectAll();
    }


    /**
     * 发帖子
     *
     * @param info 帖子
     * @return
     * @throws IOException
     */
    @Async("threadPoolExecutor")
    @Override
    public Future<Integer> add(Info info) {
        if (info == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        int result = infoMapper.addInfo(info);
        // 此时 info 对象的 id 字段已经被设置为生成的主键值
        return new AsyncResult<>(result);
    }

    @Async("threadPoolExecutor")
    @Override
    public Future<Integer> updateById(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return new AsyncResult<>(infoMapper.updateById(id));
    }

    @Override
    public Info selectById(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return infoMapper.selectById(id);
    }

    @Override
    public List<Info> selectInfos(Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return infoMapper.selectInfos(userId);
    }

    @Async("threadPoolExecutor")
    @Override
    public Future<Integer> deleteInfoWithComments(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        int i = infoMapper.deleteWithComments(id);
        return new AsyncResult<>(i == 0 ? 0 : 1);
    }

    @Override
    public Future<Integer> updateComments(Integer pubId) {
        if (pubId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return new AsyncResult<>(infoMapper.updateComments(pubId));
    }

    /**
     * 获取所有帖子和评论
     *
     * @return 一个集合
     */
    @Override
    public List<InfoWithCommentsDTO> getAllInfoAndComments() {
        List<InfoWithCommentsDTO> result = new ArrayList<>();
        List<Info> infoList = this.getData();
        for (Info info : infoList) {
            List<CommentInfo> comments = commentService.selectByInfoId(info.getId());
            InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
            result.add(dto);
        }
        return result;
    }

    @Override
    public List<InfoWithCommentsDTO> getDtoByUserId(Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<InfoWithCommentsDTO> result = new ArrayList<>();
        List<Info> infoList = selectInfos(userId);
        for (Info info : infoList) {
            List<CommentInfo> comments = commentService.selectByInfoId(info.getId());
            InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
            result.add(dto);
        }
        return result;
    }
}

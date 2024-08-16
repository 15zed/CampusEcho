package com.hgc.campusechocommentservice.service.impl;


import com.hgc.campusechocommentservice.mapper.CommentMapper;
import com.hgc.campusechocommentservice.service.RedisService;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.sankuai.inf.leaf.service.SegmentService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
@Service
@DubboService
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RedisService redisService;
    @Autowired
    private SegmentService segmentService;

    @Override
    public Future<Integer> add(CommentInfo comment) {
        if (comment == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // 生成唯一id
        long id = segmentService.getId("comment").getId();
        comment.setCommentId((int) id);
        return new AsyncResult<>(commentMapper.add(comment));
    }

    @Override
    public List<CommentInfo> selectByInfoId(Integer id, Long start, Long end) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return commentMapper.selectByInfoId(id, start, end);
    }

    @Override
    public List<Integer> selectIdsByInfoId(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return commentMapper.selectIdListByInfoId(id);
    }

    @Override
    public CommentInfo selectById(Integer commentId) {
        if (commentId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return commentMapper.selectById(commentId);
    }

    @Override
    public String generateUniqueId(CommentInfo comment) {
        if (comment == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // 将评论信息转换成字符串
        String commentString = comment.toString();
        try {
            // 创建 MessageDigest 对象，指定使用 SHA-256 算法进行哈希计算
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 对帖子进行哈希计算
            byte[] hashBytes = digest.digest(commentString.getBytes());
            // 使用 Base64 编码将哈希结果转换成字符串
            String hashString = Base64.getEncoder().encodeToString(hashBytes);
            // 将评论人id与哈希字符串拼接起来作为唯一标识符
            return comment.getUserId() + "-" + hashString;
        }
        catch (NoSuchAlgorithmException e) {
            // 哈希算法不支持时抛出异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法生成唯一Id");
        }
    }

    @Override
    public List<CommentInfo> selectPage(Integer infoId, Integer pageNum) {
        if (infoId == null || pageNum == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        long start = (pageNum - 1) * 10L;//0~9 10~19 20~29
        long end = start + 9;
        if (pageNum <= 10) {
            //去缓存查
            List<Integer> commentIdList = redisService.getIds(infoId, start, end);
            //没有去MySQL查，写入缓存
            if (commentIdList.isEmpty()) {
                List<CommentInfo> commentsList = commentMapper.selectByInfoId(infoId, start, end);
                for (CommentInfo commentInfo : commentsList) {
                    redisService.add(commentInfo);
                    commentIdList.add(commentInfo.getCommentId());
                }
                return commentsList;
            }else {
                return redisService.getComments(commentIdList);
            }
        }else {
            //去数据库查,不用写入缓存
            return commentMapper.selectByInfoId(infoId, start, end);
        }
    }

    @Override
    public void deleteAll(List<Integer> commentIds) {
        commentMapper.deleteAll(commentIds);
    }
}

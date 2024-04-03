package com.hgc.school.service.impl;

import com.hgc.school.commons.ErrorCode;
import com.hgc.school.exception.BusinessException;
import com.hgc.school.mapper.CommentMapper;
import com.hgc.school.service.CommentService;
import com.hgc.school.vo.CommentInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public Future<Integer> add(CommentInfo comment) {
        if(comment == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return new AsyncResult<>(commentMapper.add(comment));
    }

    @Override
    public List<CommentInfo> selectByInfoId(Integer id) {
        if(id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return commentMapper.selectByInfoId(id);
    }

    @Override
    public List<Integer> selectIdsByInfoId(Integer id) {
        if(id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return commentMapper.selectIdListByInfoId(id);
    }

    @Override
    public CommentInfo selectById(Integer commentId) {
        if(commentId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return commentMapper.selectById(commentId);
    }
}

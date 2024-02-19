package com.hgc.school.service.impl;

import com.hgc.school.mapper.CommentMapper;
import com.hgc.school.service.CommentService;
import com.hgc.school.vo.CommentInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */
@Service
public class CommentServiceImpl implements CommentService {

    @Autowired
    private CommentMapper commentMapper;

    @Override
    public CommentInfo add(CommentInfo comment) {
         commentMapper.add(comment);
        // 此时 commentInfo 对象的 commentId 字段已经被设置为生成的主键值
         return comment;
    }

    @Override
    public List<CommentInfo> selectByInfoId(Integer id) {
        return commentMapper.selectByInfoId(id);
    }
}

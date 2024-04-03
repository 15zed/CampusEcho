package com.hgc.school.service;

import com.hgc.school.vo.CommentInfo;

import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
public interface CommentService {
    Future<Integer> add(CommentInfo comment);

    List<CommentInfo> selectByInfoId(Integer id);

    List<Integer> selectIdsByInfoId(Integer id);

    CommentInfo selectById(Integer commentId);
}

package com.hgc.school.service;

import com.hgc.school.vo.CommentInfo;

import java.util.List;

/**
 *
 */
public interface CommentService {
    CommentInfo add(CommentInfo comment);

    List<CommentInfo> selectByInfoId(Integer id);
}

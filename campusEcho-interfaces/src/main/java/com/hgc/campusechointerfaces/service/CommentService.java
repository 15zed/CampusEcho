package com.hgc.campusechointerfaces.service;



import com.hgc.campusechomodel.entity.CommentInfo;

import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
public interface CommentService {
    Future<Integer> add(CommentInfo comment);

    List<CommentInfo> selectByInfoId(Integer id,Long start,Long end);

    List<Integer> selectIdsByInfoId(Integer id);

    CommentInfo selectById(Integer commentId);

    String generateUniqueId(CommentInfo comment);

    List<CommentInfo> selectPage(Integer infoId, Integer pageNum);

    void deleteAll(List<Integer> commentIds);
}

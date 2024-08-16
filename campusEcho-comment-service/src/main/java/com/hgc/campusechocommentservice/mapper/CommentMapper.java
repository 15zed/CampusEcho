package com.hgc.campusechocommentservice.mapper;


import com.hgc.campusechomodel.entity.CommentInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 *
 */
@Mapper
public interface CommentMapper {

    int add(CommentInfo comment);

    void batchAdd(@Param("comments") List<CommentInfo> comments);

    List<CommentInfo> selectByInfoId(Integer id,Long start,Long end);

    List<CommentInfo> selectAll();

    List<Integer> selectIdListByInfoId(Integer id);

    CommentInfo selectById(Integer commentId);

    void deleteByCommentId(Integer commentId);

    void deleteAll(List<Integer> commentIds);
}

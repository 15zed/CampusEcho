package com.hgc.campusechosearchandrecommendservice.mapper;

import com.hgc.campusechomodel.entity.CommentInfo;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 *
 */
@Mapper
public interface CommentMapper {

    int add(CommentInfo comment);

    List<CommentInfo> selectByInfoId(Integer id);

    List<CommentInfo> selectAll();

    List<Integer> selectIdListByInfoId(Integer id);

    CommentInfo selectById(Integer commentId);
}

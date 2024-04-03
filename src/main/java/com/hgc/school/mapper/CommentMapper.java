package com.hgc.school.mapper;

import com.hgc.school.vo.CommentInfo;
import org.apache.ibatis.annotations.Mapper;


import java.util.List;

/**
 *
 */
@Mapper
public interface CommentMapper {
    //开启主键生成 将生成的主键赋值给commentInfo对象的commentId属性
//    @Options(useGeneratedKeys = true, keyProperty = "commentId")
    int add(CommentInfo comment);

    List<CommentInfo> selectByInfoId(Integer id);

    List<CommentInfo> selectAll();

    List<Integer> selectIdListByInfoId(Integer id);

    CommentInfo selectById(Integer commentId);
}

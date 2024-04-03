package com.hgc.school.service;

import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.vo.Flag;
import com.hgc.school.vo.Info;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
public interface InfoService {


    List<Info> getData();

    Future<Integer> add(Info info) ;

    Future<Integer> updateById(Integer id);

    Info selectById(Integer id);

    List<Info> selectInfos(Integer userId);

    Future<Integer> deleteInfoWithComments(Integer id);

    Future<Integer> updateComments(Integer pubId);



    /**
     * 获取所有帖子和评论
     * @return 一个集合
     */
    List<InfoWithCommentsDTO> getAllInfoAndComments();

    List<InfoWithCommentsDTO> getDtoByUserId(Integer userId);
}

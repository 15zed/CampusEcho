package com.hgc.campusechointerfaces.service;

import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 *
 */
public interface ESService {

    List<InfoWithCommentsDTO> search(String keyword);

    <T> Future<Integer> add(String index, T data);

    Future<Integer> update(String index, String id, Map<String, Object> doc);
    Future<Integer> delete(String infoId,List<Integer> commentIdList);

    String getInfoById(String infoId);

    String getCommentById(String commentId);

    Future<Integer> deleteOne(String commentIndex, Integer commentId);

    Future<Integer> deleteAll(String commentIndex, List<Integer> commentIds);
}

package com.hgc.school.service;

import com.hgc.school.dto.InfoWithCommentsDTO;

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
}

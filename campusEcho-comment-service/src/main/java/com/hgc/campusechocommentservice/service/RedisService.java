package com.hgc.campusechocommentservice.service;


import com.hgc.campusechomodel.entity.CommentInfo;

import java.util.List;
import java.util.concurrent.Future;

/**
 *
 */
public interface RedisService {

    <T> Future<Integer> add(CommentInfo comment);

     Future<Integer> delete(Integer commentId);

     boolean setUniqueId(String uniqueId);

    List<Integer> getIds(Integer infoId,long start,long end);

    List<CommentInfo> getComments(List<Integer> commentIdList);
}

package com.hgc.school.service;

import com.hgc.school.vo.Info;

import java.util.List;

/**
 *
 */
public interface InfoService {
    List<Info> getData();

    Info add(Info info);

    void updateById(Integer id);

    Info selectById(Integer id);

    List<Info> selectInfos(Integer userId);

    void deleteInfoWithComments(Integer id);

    void updateComments(Integer pubId);

    List<Integer> selectAllId();
}

package com.hgc.campusechointerfaces.service;

import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.Info;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;


/**
 *
 */
public interface InfoService {


    List<Info> getData(int start, int end);

    Integer add(Info info) ;

    Future<Integer> updateById(Integer id);
    Future<Integer> decreaseUpdateById(Integer id);

    Info selectById(Integer id);

    List<Info> selectInfos(Integer userId);

    Future<Integer> deleteInfoWithComments(Integer id);

    Future<Integer> updateComments(Integer pubId);



    /**
     * 获取所有帖子和评论
     * @return 一个集合
     */
    List<InfoWithCommentsDTO> getAllInfoAndComments(Integer userId, long start, long end);



    String generateUniqueId(Info info);

    void cover(Info info);

    List<Info> getInfoListByIds(Set<String> infoIdList);

    List<Info> selectInfoByPage(Integer userId, int start, int end);

    List<InfoWithCommentsDTO> getDtoByPage(int page, int size);

    InfoWithCommentsDTO getDtoById(Integer infoId);
}

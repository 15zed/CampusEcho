package com.hgc.campusechopostsservice.mapper;



import com.hgc.campusechomodel.entity.Info;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.util.Set;

/**
 *
 */
@Mapper
public interface InfoMapper {
    List<Info> selectAll(int start, int end);

    int addInfo(Info info);

    int updateById(Integer id);
    int decreaseUpdateById(Integer id);

    Info selectById(Integer id);

    List<Info> selectInfos(Integer userId);

    int deleteWithComments(Integer id);

    int updateComments(Integer pubId);

    List<Integer> selectIds();

    void cover(Info info);

    List<Info> selectByIds(Set<String> infoIdList);

    List<Info> selectByPage(Integer userId, int start, int end);
}

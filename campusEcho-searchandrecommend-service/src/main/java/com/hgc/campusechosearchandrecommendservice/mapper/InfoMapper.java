package com.hgc.campusechosearchandrecommendservice.mapper;

import com.hgc.campusechomodel.entity.Info;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

/**
 *
 */
@Mapper
public interface InfoMapper {
    List<Info> selectAll();
    //开启主键生成 将生成的主键赋值给info对象的id属性
//    @Options(useGeneratedKeys = true, keyProperty = "id")
    int addInfo(Info info);

    int updateById(Integer id);
    int decreaseUpdateById(Integer id);

    Info selectById(Integer id);

    List<Info> selectInfos(Integer userId);

    int deleteWithComments(Integer id);

    int updateComments(Integer pubId);

    List<Integer> selectIds();

    void cover(Info info);
}

package com.hgc.school.mapper;

import com.hgc.school.vo.Flag;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 *
 */
@Mapper
public interface FlagMapper {

    void addFlag(Flag flag);

    List<Flag> selectNeeds();
}

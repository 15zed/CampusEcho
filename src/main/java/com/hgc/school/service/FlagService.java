package com.hgc.school.service;

import com.hgc.school.vo.Flag;

import java.util.List;

/**
 *
 */
public interface FlagService {

    void addFlag(Flag flag);

    List<Flag> selectNeeds();
}

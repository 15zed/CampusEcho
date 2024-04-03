package com.hgc.school.service.impl;

import com.hgc.school.commons.ErrorCode;
import com.hgc.school.exception.BusinessException;
import com.hgc.school.mapper.FlagMapper;
import com.hgc.school.service.FlagService;
import com.hgc.school.vo.Flag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */
@Service
public class FlagServiceImpl implements FlagService {
    @Autowired
    private FlagMapper flagMapper;

    @Override
    public void addFlag(Flag flag) {
        if (flag == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        flagMapper.addFlag(flag);
    }

    @Override
    public List<Flag> selectNeeds() {
        return flagMapper.selectNeeds();
    }
}

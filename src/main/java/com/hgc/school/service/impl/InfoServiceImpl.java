package com.hgc.school.service.impl;

import com.hgc.school.mapper.InfoMapper;
import com.hgc.school.service.InfoService;
import com.hgc.school.vo.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 *
 */
@Service
public class InfoServiceImpl implements InfoService {
    @Autowired
    private InfoMapper infoMapper;

    @Override
    public List<Info> getData() {
        return infoMapper.selectAll();
    }

    @Override
    public Info add(Info info) {
        infoMapper.addInfo(info);
        // 此时 info 对象的 id 字段已经被设置为生成的主键值
        return info;
    }

    @Override
    public void updateById(Integer id) {
        infoMapper.updateById(id);
    }

    @Override
    public Info selectById(Integer id) {
        return infoMapper.selectById(id);
    }

    @Override
    public List<Info> selectInfos(Integer userId) {
        return infoMapper.selectInfos(userId);
    }

    @Override
    public void deleteInfoWithComments(Integer id) {
        infoMapper.deleteWithComments(id);
    }

    @Override
    public void updateComments(Integer pubId) {
        infoMapper.updateComments(pubId);
    }

    @Override
    public List<Integer> selectAllId() {
        return infoMapper.selectIds();
    }
}

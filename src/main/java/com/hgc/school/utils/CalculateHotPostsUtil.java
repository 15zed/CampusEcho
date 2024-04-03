package com.hgc.school.utils;


import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.mapper.CommentMapper;
import com.hgc.school.mapper.InfoMapper;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.JedisPool;

import java.util.*;


/**
 *
 */
@Component
public class CalculateHotPostsUtil {
    @Autowired
    private InfoMapper infoMapper;
    @Autowired
    private CommentMapper commentMapper;

    public List<InfoWithCommentsDTO> calculate() {
        double heat;
        int count = 0;
        ArrayList<InfoWithCommentsDTO> result = new ArrayList<>();
        TreeMap<Double, InfoWithCommentsDTO> map = new TreeMap<>(Comparator.reverseOrder());
        //先查询所有的数据
        List<Info> infoList = infoMapper.selectAll();
        for (Info info : infoList) {
            //根据 公式：热度 = 评论数 * 0.6 + 点赞数 * 0.4 计算每条帖子的热度
            heat = info.getComments() * 0.6 + info.getLikes() * 0.4;
            List<CommentInfo> commentsList = commentMapper.selectByInfoId(info.getId());
            InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, commentsList);
            map.put(heat, dto);
        }
        //返回前16条帖子数据
        Iterator<Map.Entry<Double, InfoWithCommentsDTO>> iterator = map.entrySet().iterator();
        while (iterator.hasNext() && count < 16) {
            InfoWithCommentsDTO dto = iterator.next().getValue();
            result.add(dto);
            count++;
        }
        return result;
    }
}

package com.hgc.school.utils;

import com.alibaba.fastjson.JSON;
import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.mapper.CommentMapper;
import com.hgc.school.mapper.InfoMapper;
import com.hgc.school.mapper.UserMapper;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@Component
public class CalculateHotPostsUtil {
    @Autowired
    private  InfoMapper infoMapper;
    @Autowired
    private  CommentMapper commentMapper;
    @Autowired
    private JedisPool jedisPool;

//    public  List<InfoWithCommentsDTO> calculate(){
//        double heat;
//        int count = 0;
//        ArrayList<InfoWithCommentsDTO> result = new ArrayList<>();
//        TreeMap<Double,InfoWithCommentsDTO> map = new TreeMap<>(Comparator.reverseOrder());
//        //先查询所有的数据
//        List<Info> infoList = infoMapper.selectAll();
//        for (Info info : infoList) {
//            //根据 公式：热度 = 评论数 * 0.6 + 点赞数 * 0.4 计算每条帖子的热度
//            heat = info.getComments() * 0.6 + info.getLikes() * 0.4;
//            List<CommentInfo> commentsList = commentMapper.selectByInfoId(info.getId());
//            InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info,commentsList);
//            map.put(heat,dto);
//        }
//        //返回前16条帖子数据
//        Iterator<Map.Entry<Double, InfoWithCommentsDTO>> iterator = map.entrySet().iterator();
//        while (iterator.hasNext() && count < 16){
//            InfoWithCommentsDTO dto = iterator.next().getValue();
//            result.add(dto);
//            count++;
//        }
//        return result;
//    }

    public  List<InfoWithCommentsDTO> calculate(){
        double heat;
        int count = 0;
        ArrayList<InfoWithCommentsDTO> result = new ArrayList<>();
        TreeMap<Double,InfoWithCommentsDTO> map = new TreeMap<>(Comparator.reverseOrder());
        //先查询所有的数据
        List<Integer> infoIdList = infoMapper.selectIds();
        Jedis jedis = jedisPool.getResource();
        for (Integer infoId : infoIdList) {
            Info info = JSON.parseObject(jedis.get("info:" + infoId), Info.class);
            //根据 公式：热度 = 评论数 * 0.6 + 点赞数 * 0.4 计算每条帖子的热度
            heat = info.getComments() * 0.6 + info.getLikes() * 0.4;
            List<String> commentIdList = jedis.lrange("comment:pubId:" + infoId, 0, -1);
            List<CommentInfo> commentsList = new ArrayList<>();
            for (String commentId : commentIdList) {
                CommentInfo commentInfo = JSON.parseObject(jedis.get("comment:" + commentId), CommentInfo.class);
                commentsList.add(commentInfo);
            }
            InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info,commentsList);
            map.put(heat,dto);
        }
        //返回前16条帖子数据
        Iterator<Map.Entry<Double, InfoWithCommentsDTO>> iterator = map.entrySet().iterator();
        while (iterator.hasNext() && count < 16){
            InfoWithCommentsDTO dto = iterator.next().getValue();
            result.add(dto);
            count++;
        }
        jedis.close();
        return result;
    }
}

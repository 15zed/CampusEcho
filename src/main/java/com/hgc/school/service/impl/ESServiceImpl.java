package com.hgc.school.service.impl;

import com.alibaba.fastjson.JSON;
import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.service.ESService;
import com.hgc.school.utils.ESIndexUtil;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;


/**
 *
 */
@Service
@Slf4j
public class ESServiceImpl implements ESService {
    @Autowired
    private RestHighLevelClient restHighLevelClient;
    @Autowired
    private JedisPool jedisPool;


    @Override
    public List<InfoWithCommentsDTO> search(String keyword) {
        //新建查询请求
        SearchRequest searchRequest = new SearchRequest(ESIndexUtil.INFO_INDEX, ESIndexUtil.COMMENT_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // match 查询，匹配字段
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("text", keyword);
        // 设置查询构建器
        sourceBuilder.query(matchQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));
        // 配置高亮
//        HighlightBuilder highlightBuilder = new HighlightBuilder();
//        highlightBuilder.field("text");
//        highlightBuilder.preTags("<span style = 'color:red'>");
//        highlightBuilder.postTags("</span>");
//        sourceBuilder.highlighter(highlightBuilder);
        // 将查询构建器添加到搜索请求
        searchRequest.source(sourceBuilder);
        // 执行搜索请求
        SearchResponse searchResponse = null;
        try {
            searchResponse = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 处理搜索结果
            List<InfoWithCommentsDTO> dtoList = processSearchResults(searchResponse,keyword);
            return dtoList;
    }

    public List<InfoWithCommentsDTO> processSearchResults(SearchResponse searchResponse,String keyword) {
        Set<InfoWithCommentsDTO> uniqueResults = new HashSet<>();

        // 处理搜索结果
        SearchHit[] hits = searchResponse.getHits().getHits();
        log.info("hitsSize:"+hits.length);
        Jedis jedis = jedisPool.getResource();
        for (SearchHit hit : hits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
            String sourceAsString = hit.getSourceAsString();

            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            HighlightField text = highlightFields.get("text");


            //匹配评论
            if(sourceAsMap.size() == 5){
                List<CommentInfo> comments =new ArrayList<>();
                Integer pubId = (Integer) sourceAsMap.get("pubId");//帖子id
                Info info = JSON.parseObject(jedis.get("info:" + pubId), Info.class);
                if(info.getText().contains(keyword)){
//                    log.info("Info改之前："+info.getText());
                    info.setText(info.getText().replaceAll(keyword, "<span style = 'color:red'>" + keyword + "</span>"));
//                    log.info("Info改之后："+info.getText());
                }
                List<String> commentIdList = jedis.lrange("comment:pubId:" + pubId, 0, -1);
                for (String commentId : commentIdList) {
                    CommentInfo commentInfo = JSON.parseObject(jedis.get("comment:" + commentId), CommentInfo.class);
                    System.out.println(commentInfo.toString());
                    //处理高亮显示
//                    if (text != null && commentInfo.getText().contains(keyword)) {
//                        Text[] fragments = text.fragments();//钢笔1
//                        StringBuilder newText = new StringBuilder();
//                        for (Text text1 : fragments) {
//                            newText.append(text1);
//                        }
//                        System.out.println(newText.toString());
//                        commentInfo.setText(newText.toString());
//                        log.info("comment改之后："+commentInfo.getText());
//                    }
                    if(commentInfo.getText().contains(keyword)){
//                        log.info("comment该以前！！："+commentInfo.getText());
                        commentInfo.setText(commentInfo.getText().replaceAll(keyword, "<span style = 'color:red'>" + keyword + "</span>"));
//                        log.info("comment改以后!!："+commentInfo.getText());
                    }

                    comments.add(commentInfo);
                }




                InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
                log.info("匹配评论组装的dto:"+dto);
                uniqueResults.add(dto);
                log.info("set大小："+uniqueResults.size());
            }else {
                //匹配帖子
                Info info = JSON.parseObject(sourceAsString, Info.class);
                System.out.println("info:"+info.toString());
                List<CommentInfo> comments =new ArrayList<>();
                Integer infoId = info.getId();//帖子id
                List<String> commentIdList = jedis.lrange("comment:pubId:" + infoId, 0, -1);
                for (String commentId : commentIdList) {
                    CommentInfo commentInfo = JSON.parseObject(jedis.get("comment:" + commentId), CommentInfo.class);
                    if(commentInfo.getText().contains(keyword)){
                        log.info("comment该以前！！："+commentInfo.getText());
                        commentInfo.setText(commentInfo.getText().replaceAll(keyword, "<span style = 'color:red'>" + keyword + "</span>"));
                        log.info("comment改以后!!："+commentInfo.getText());
                    }
                    comments.add(commentInfo);
                }
                //处理高亮显示
//                if (text != null) {
//                    Text[] fragments = text.fragments();
//                    StringBuilder newText = new StringBuilder();
//                    for (Text text1 : fragments) {
//                        newText.append(text1);
//                    }
//                    info.setText(newText.toString());
//                    log.info("Info改之后！！："+info.getText());
//                }
                info.setText(info.getText().replaceAll(keyword,"<span style = 'color:red'>" + keyword + "</span>"));


                InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
                log.info("匹配帖子组装的dto："+dto);
                uniqueResults.add(dto);
                log.info("set大小："+uniqueResults.size());
            }


        }
        jedis.close();
        System.out.println("haha");
        return new ArrayList<>(uniqueResults);
    }
}




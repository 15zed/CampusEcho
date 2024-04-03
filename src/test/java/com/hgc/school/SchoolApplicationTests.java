package com.hgc.school;

import com.alibaba.fastjson.JSON;
import com.hgc.school.mapper.UserMapper;
import com.hgc.school.utils.ESIndexUtil;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@Slf4j
class SchoolApplicationTests {
    @Autowired
    RestHighLevelClient restHighLevelClient;
    @Autowired
    UserMapper userMapper;

    //测试创建索引
    @Test
    void contextLoads() throws IOException {
        CreateIndexRequest request = new CreateIndexRequest("test3");
        CreateIndexResponse response = restHighLevelClient.indices().create(request, RequestOptions.DEFAULT);
        System.out.println(response);
    }

    //测试获取索引 索引相当于数据库 只能判断是否存在
    @Test
    void test1() throws IOException {
        GetIndexRequest request = new GetIndexRequest("test3");
        boolean exists = restHighLevelClient.indices().exists(request, RequestOptions.DEFAULT);
        System.out.println(exists);
    }

    //测试删除索引
    @Test
    void test2() throws IOException {
        DeleteIndexRequest request = new DeleteIndexRequest("test3");
        AcknowledgedResponse response = restHighLevelClient.indices().delete(request, RequestOptions.DEFAULT);
        System.out.println(response);
        System.out.println(response.isAcknowledged());
    }

    //测试创建文档 PUT /test3/t_user/1
    @Test
    void test3() throws IOException {
        User user = userMapper.selectById(8);
        IndexRequest request = new IndexRequest("test3");
        request.id(String.valueOf(user.getUserId()));
        request.source(JSON.toJSONString(user), XContentType.JSON);
        IndexResponse response = restHighLevelClient.index(request, RequestOptions.DEFAULT);
        System.out.println(response.toString());
    }

    //测试获取文档
    @Test
    void test4() throws IOException {
        GetRequest request = new GetRequest("test3", "8");//查询test3中 id为8的数据
        GetResponse response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
        System.out.println(response.getSourceAsString());//获取source中的数据 只包含数据库中的数据
        System.out.println(response);//获取所有数据 就是kibana get查出的结果
    }

    //测试删除文档
    @Test
    void test5() throws IOException {
        DeleteRequest request = new DeleteRequest("test3", "1");
        DeleteResponse response = restHighLevelClient.delete(request, RequestOptions.DEFAULT);
        System.out.println(response.status());
    }

    //测试查询文档
    @Test
    void test6() throws IOException {
        SearchRequest request = new SearchRequest("test3");
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        MatchQueryBuilder queryCondition = QueryBuilders.matchQuery("username", "小王");
        searchSourceBuilder.query(queryCondition);
        request.source(searchSourceBuilder);
        SearchResponse response = restHighLevelClient.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            System.out.println(hit.getSourceAsString());
            //{"area":"北京市朝阳区","contact":"1578485752@mail.com","fans":"9","follows":"1,2,10,9","head":"3.jpg","likelist":"2,1,37,33,36,3","password":"123","sex":"男","status":1,"userId":8,"username":"小王"}
        }
//        for (SearchHit hit : hits) {
//            Map<String, Object> sourceAsMap = hit.getSourceAsMap();
//            for (Map.Entry<String, Object> entry : sourceAsMap.entrySet()) {
//                System.out.println(entry.getKey()+":"+entry.getValue());
//                //area:北京市朝阳区
//                //head:3.jpg
//                //likelist:2,1,37,33,36,3
//                //password:123
//                //contact:1578485752@mail.com
//                //sex:男
//                //follows:1,2,10,9
//                //userId:8
//                //fans:9
//                //status:1
//                //username:小王
//            }
//        }
    }

    @Test
    void test7() throws IOException {
        SearchRequest searchRequest = new SearchRequest(ESIndexUtil.INFO_INDEX, ESIndexUtil.COMMENT_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // 在 should 子句中添加 match 查询，分别匹配不同的字段
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("text", "快乐");
        // 设置查询构建器
        sourceBuilder.query(matchQueryBuilder);
        // 配置高亮
        HighlightBuilder highlightBuilder = new HighlightBuilder();
        highlightBuilder.field("text");
//        highlightBuilder.preTags("<span style = 'color:red'>");
//        highlightBuilder.postTags("</span>");
        sourceBuilder.highlighter(highlightBuilder);
        // 将查询构建器添加到搜索请求
        searchRequest.source(sourceBuilder);
        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            System.out.println("hit:" + sourceAsString);
            if (hit.getSourceAsMap().size() == 5) {
                Integer pubId = (Integer) hit.getSourceAsMap().get("pubId");
                System.out.println("Comment:" + pubId);
            } else {
                System.out.println("Info:" + JSON.parseObject(sourceAsString, Info.class));
            }


        }
    }

    //测试更新文档
    @Test
    void test8() throws IOException {
        // 构建更新请求
        UpdateRequest updateRequest = new UpdateRequest(ESIndexUtil.COMMENT_INDEX, "1");

        // 构建部分更新的文档
        Map<String, Object> doc = new HashMap<>();
        doc.put("text", "哈哈nm");

        // 使用doc方法进行部分更新
        updateRequest.doc(doc, XContentType.JSON);

        // 执行更新请求
        UpdateResponse updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println(updateResponse.status());
    }

    @Test
    void test9() throws IOException {
        SearchRequest searchRequest = new SearchRequest(ESIndexUtil.INFO_INDEX);
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        searchSourceBuilder.size(3000);
        MatchAllQueryBuilder allQueryBuilder = QueryBuilders.matchAllQuery();
        searchSourceBuilder.query(allQueryBuilder);
//        searchSourceBuilder.timeout(new TimeValue(30, TimeUnit.SECONDS));
        searchRequest.source(searchSourceBuilder);


        SearchResponse response = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

        SearchHit[] hits = response.getHits().getHits();
        System.out.println(hits.length);
        for (SearchHit hit : hits) {
            System.out.println(hit.getSourceAsString());
        }
    }

    @Test
    void test10() throws IOException {
        // 构建更新请求
        UpdateRequest updateRequest = new UpdateRequest(ESIndexUtil.INFO_INDEX, "106");

        // 构建部分更新的文档
        Map<String, Object> doc = new HashMap<>();
        Integer likes = 0;
        doc.put("likes", likes);

        // 使用doc方法进行部分更新
        updateRequest.doc(doc, XContentType.JSON);

        // 执行更新请求
        UpdateResponse updateResponse = restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
        System.out.println(updateResponse.status());
        RestStatus status = updateResponse.status();
        int status1 = status.getStatus();
    }
}

package com.hgc.campusechosearchandrecommendservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.constant.ESConstant;
import com.hgc.campusechocommon.constant.OperationResultConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.ESService;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechosearchandrecommendservice.mapper.CommentMapper;
import com.hgc.campusechosearchandrecommendservice.mapper.InfoMapper;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboService;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.core.TimeValue;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


/**
 *
 */
@Service
@Slf4j
@DubboService
public class ESServiceImpl implements ESService {
    @Autowired
    private InfoMapper infoMapper;
    @Autowired
    private CommentMapper commentMapper;
    @Autowired
    private RestHighLevelClient restHighLevelClient;


    @Override
    public List<InfoWithCommentsDTO> search(String keyword) {
        if (keyword == null || keyword.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        //新建查询请求
        SearchRequest searchRequest = new SearchRequest(ESConstant.INFO_INDEX, ESConstant.COMMENT_INDEX);
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        // match 查询，匹配字段
        MatchQueryBuilder matchQueryBuilder = QueryBuilders.matchQuery("text", keyword);
        // 设置查询构建器
        sourceBuilder.query(matchQueryBuilder);
        sourceBuilder.timeout(new TimeValue(60, TimeUnit.SECONDS));

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
        List<InfoWithCommentsDTO> dtoList = processSearchResults(searchResponse, keyword);
        return dtoList;
    }

    @Async("threadPoolExecutor")
    @Override
    public <T> Future<Integer> add(String index, T data) {
        try {
            if (index == null || data == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
            String id;
            String source;
            if (data instanceof Info) {
                Info info = (Info) data;
                id = String.valueOf(info.getId());
                source = JSON.toJSONString(info);
            } else if (data instanceof CommentInfo) {
                CommentInfo comment = (CommentInfo) data;
                id = String.valueOf(comment.getCommentId());
                source = JSON.toJSONString(comment);
            } else {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的data类型 " + data.getClass().getName());
            }
            IndexRequest request = new IndexRequest(index);
            request.id(id);
            request.source(source, XContentType.JSON);
            restHighLevelClient.index(request, RequestOptions.DEFAULT);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        } catch (IOException e) {
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }

    @Override
    public Future<Integer> update(String index, String id, Map<String, Object> doc) {
        try {
            if (index == null || id == null || doc.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR);
            UpdateRequest updateRequest = new UpdateRequest(index, id);
            updateRequest.doc(doc, XContentType.JSON);
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        } catch (IOException e) {
            log.error(e.getMessage());
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }

    @Override
    public Future<Integer> delete(String infoId, List<Integer> commentIdList) {
        try {
            if (infoId == null || commentIdList.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR);
            BulkRequest bulkRequest = new BulkRequest();
            DeleteRequest deleteRequest = new DeleteRequest(ESConstant.INFO_INDEX, infoId);
            for (Integer commentId : commentIdList) {
                DeleteRequest req = new DeleteRequest(ESConstant.COMMENT_INDEX, String.valueOf(commentId));
                bulkRequest.add(req);
            }
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
            restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        } catch (IOException e) {
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }

    @Override
    public Future<Integer> deleteAll(String commentIndex, List<Integer> commentIds) {
        try {
            if (commentIndex == null || commentIds.isEmpty()) throw new BusinessException(ErrorCode.PARAMS_ERROR);
            BulkRequest bulkRequest = new BulkRequest();
            for (Integer commentId : commentIds) {
                DeleteRequest req = new DeleteRequest(ESConstant.COMMENT_INDEX, String.valueOf(commentId));
                bulkRequest.add(req);
            }
            restHighLevelClient.bulk(bulkRequest, RequestOptions.DEFAULT);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        } catch (IOException e) {
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }

    @Override
    public Future<Integer> deleteOne(String commentIndex, Integer commentId) {
        try {
            if (commentId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
            DeleteRequest deleteRequest = new DeleteRequest(ESConstant.COMMENT_INDEX, String.valueOf(commentId));
            restHighLevelClient.delete(deleteRequest, RequestOptions.DEFAULT);
            return new AsyncResult<>(OperationResultConstant.SUCCESS);
        }catch (IOException e) {
            return new AsyncResult<>(OperationResultConstant.FAILURE);
        }
    }

    @Override
    public String getInfoById(String infoId) {
        GetResponse response;
        try {
            if (infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
            GetRequest request = new GetRequest(ESConstant.INFO_INDEX, infoId);
            response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
            return response.getSourceAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getCommentById(String commentId) {
        GetResponse response;
        try {
            if (commentId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
            GetRequest request = new GetRequest(ESConstant.COMMENT_INDEX, commentId);
            response = restHighLevelClient.get(request, RequestOptions.DEFAULT);
            return response.getSourceAsString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public List<InfoWithCommentsDTO> processSearchResults(SearchResponse searchResponse, String keyword) {
        if (searchResponse == null || keyword == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        Set<InfoWithCommentsDTO> uniqueResults = new HashSet<>();
        // 处理搜索结果
        SearchHit[] hits = searchResponse.getHits().getHits();
        log.info("hitsSize:" + hits.length);
        for (SearchHit hit : hits) {
            Map<String, Object> sourceAsMap = hit.getSourceAsMap();//key 代表文档中的字段名，而 value 则是该字段对应的值
            String sourceAsString = hit.getSourceAsString();//getSourceAsString()返回这个文档的原始数据，json格式
            //匹配评论
            if (sourceAsMap.size() == 5) {
                List<CommentInfo> comments = new ArrayList<>();
                Integer pubId = (Integer) sourceAsMap.get("pubId");//帖子id
                Info info = infoMapper.selectById(pubId);
                if (info.getText().contains(keyword)) {
//                    log.info("Info改之前："+info.getText());
                    info.setText(info.getText().replaceAll(keyword, "<span style = 'color:red'>" + keyword + "</span>"));
//                    log.info("Info改之后："+info.getText());
                }
                List<CommentInfo> commentInfoList = commentMapper.selectByInfoId(pubId);
                for (CommentInfo commentInfo : commentInfoList) {
                    if (commentInfo.getText().contains(keyword)) {
                        commentInfo.setText(commentInfo.getText().replaceAll(keyword, "<span style = 'color:red'>" + keyword + "</span>"));
                    }
                    comments.add(commentInfo);
                }
                InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
                log.info("匹配评论组装的dto:" + dto);
                uniqueResults.add(dto);
                log.info("set大小：" + uniqueResults.size());
            } else {
                //匹配帖子
                Info info = JSON.parseObject(sourceAsString, Info.class);
                System.out.println("info:" + info.toString());
                List<CommentInfo> comments = new ArrayList<>();
                Integer infoId = info.getId();//帖子id
                List<CommentInfo> commentInfoList = commentMapper.selectByInfoId(infoId);
                for (CommentInfo commentInfo : commentInfoList) {
                    if (commentInfo.getText().contains(keyword)) {
                        log.info("comment该以前！！：" + commentInfo.getText());
                        commentInfo.setText(commentInfo.getText().replaceAll(keyword, "<span style = 'color:red'>" + keyword + "</span>"));
                        log.info("comment改以后!!：" + commentInfo.getText());
                    }
                    comments.add(commentInfo);
                }
                info.setText(info.getText().replaceAll(keyword, "<span style = 'color:red'>" + keyword + "</span>"));
                InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
                log.info("匹配帖子组装的dto：" + dto);
                uniqueResults.add(dto);
                log.info("set大小：" + uniqueResults.size());
            }
        }
        return new ArrayList<>(uniqueResults);
    }
}




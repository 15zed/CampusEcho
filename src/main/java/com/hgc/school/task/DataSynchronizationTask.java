package com.hgc.school.task;

import com.alibaba.fastjson.JSON;
import com.hgc.school.service.*;
import com.hgc.school.utils.ESIndexUtil;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Flag;
import com.hgc.school.vo.Info;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 *
 */
@Component
public class DataSynchronizationTask {
    @Autowired
    private FlagService flagService;
    @Autowired
    private InfoService infoService;
    @Autowired
    private ESService esService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private RedisService redisService;

    @Scheduled(fixedRate = 600000)
    public void synchronization() {
        List<Flag> flagList = flagService.selectNeeds();//统计三个标志中，包含失败的
        HashMap<String, Integer> map = new HashMap<>();
        for (Flag flag : flagList) {
            Integer type = flag.getType();//1.发帖 2.发评论 3.点赞 4.删除帖子和评论
            Integer mysqlFlag = flag.getMysqlFlag();
            Integer esFlag = flag.getEsFlag();
            Integer redisFlag = flag.getRedisFlag();
            map.put("mysqlFlag", mysqlFlag);
            map.put("redisFlag", redisFlag);
            map.put("esFlag", esFlag);
            List<String> sucsessList = getKeyByValue(map, 1);//获取代表成功的标志 0 1 2 3
            if (sucsessList.isEmpty() || sucsessList.size() == 3) return;//如果全部失败，或者全部成功，那么数据就是一致的，就不执行了
            //mysql redis es
            //发帖        1
            //评论        1
            //点赞                     1，  11
            //删贴        1
            switch (type) {
                case 1:
                    Integer infoId = flag.getId();
                    if (infoId == null || !sucsessList.contains("mysqlFlag")) {
                        //从es中获取该数据，写入mysql
                        String jsInfo = esService.getInfoById(String.valueOf(infoId));
                        Info info = JSON.parseObject(jsInfo, Info.class);
                        infoService.add(info);
                    }
                    if (!sucsessList.contains("esFlag")) {
                        //从MySQL查数据，写入es
                        Info info = infoService.selectById(infoId);
                        esService.add(ESIndexUtil.INFO_INDEX, info);
                    }
                    break;
                case 2:
                    Integer commentId = flag.getId();
                    if (commentId == null || !sucsessList.contains("mysqlFlag")) {
                        String jsComment = esService.getCommentById(String.valueOf(commentId));
                        CommentInfo comment = JSON.parseObject(jsComment, CommentInfo.class);
                        commentService.add(comment);
                        infoService.updateComments(comment.getPubId());
                    }
                    if (!sucsessList.contains("esFlag")) {
                        CommentInfo comment = commentService.selectById(commentId);
                        Integer comments = infoService.selectById(comment.getPubId()).getComments();
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("comments", comments);
                        esService.add(ESIndexUtil.COMMENT_INDEX, comment);
                        esService.update(ESIndexUtil.INFO_INDEX, String.valueOf(comment.getPubId()), doc);
                    }
                    break;
                case 3:
                    Integer infoId1 = flag.getId();
                    Integer userId = flag.getUserId();
                    if (!sucsessList.contains("mysqlFlag")) {
                        //mysql失败，删除es, redis
                        List<Integer> commentIdList = commentService.selectIdsByInfoId(infoId1);
                        esService.delete(String.valueOf(infoId1), commentIdList);
                        redisService.delete(userId);
                        break;
                    }
                    if ((!sucsessList.contains("esFlag")) && (!sucsessList.contains("redisFlag"))) {
                        //mysql成功，es和redis失败，那么同步es和redis
                        Info info = infoService.selectById(infoId1);
                        Integer likes = info.getLikes();
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("likes", likes);
                        esService.update(ESIndexUtil.INFO_INDEX, String.valueOf(infoId1), doc);
                        redisService.delete(userId);
                        break;
                    }
                    if (sucsessList.contains("redisFlag") && (!sucsessList.contains("esFlag"))) {
                        //mysql成功，redis成功，es失败，那么同步es
                        Info info = infoService.selectById(infoId1);
                        Integer likes = info.getLikes();
                        Map<String, Object> doc = new HashMap<>();
                        doc.put("likes", likes);
                        esService.update(ESIndexUtil.INFO_INDEX, String.valueOf(infoId1), doc);
                        break;
                    }
                    if (sucsessList.contains("esFlag") && (!sucsessList.contains("redisFlag"))) {
                        //mysql成功，es成功，redis失败，那么同步redis
                        redisService.delete(userId);
                    }
                    break;
                case 4:
                    Integer infoId2 = flag.getId();
                    if (!sucsessList.contains("mysqlFlag")) {
                        infoService.deleteInfoWithComments(infoId2);
                    }
                    if (!sucsessList.contains("esFlag")) {
                        List<Integer> commentIdList = commentService.selectIdsByInfoId(infoId2);
                        esService.delete(String.valueOf(infoId2), commentIdList);
                    }
                    break;
            }
        }
    }

    /**
     * 根据value 获取 key
     * @param map
     * @param value
     * @return
     * @param <K>
     * @param <V>
     */
    public static <K, V> List<K> getKeyByValue(Map<K, V> map, V value) {
        List<K> list = new ArrayList<>();
        for (Map.Entry<K, V> entry : map.entrySet()) {
            if (value.equals(entry.getValue())) {
                list.add(entry.getKey());
            }
        }
        return list;
    }
}

package com.hgc.campusechopostsservice.service.impl;

import com.alibaba.fastjson.JSON;
import com.github.benmanes.caffeine.cache.Cache;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechopostsservice.mapper.InfoMapper;
import com.hgc.campusechopostsservice.service.RedisService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.apache.dubbo.config.annotation.DubboService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 *
 */
@Service
@DubboService
public class InfoServiceImpl implements InfoService {
    @Autowired
    private InfoMapper infoMapper;
    @DubboReference
    CommentService commentService;
    @Autowired
    private Cache<String, String> caffeineCache;
    @Autowired
    private RedisService redisService;



    @Override
    public List<Info> getData(int start, int end) {
        return infoMapper.selectAll(start, end);
    }

    /**
     * 获取某个用户帖子和评论，分页
     *
     * @param start
     * @param end
     * @return 一个集合
     */
    @Override
    public List<InfoWithCommentsDTO> getAllInfoAndComments(Integer userId,long start, long end) {
        List<InfoWithCommentsDTO> infoWithCommentsDTOS = new ArrayList<>();
        // 获取用户帖子
        List<Info> infos = infoMapper.selectByPage(userId, (int) start, (int) end);

        if (infos == null || infos.size() == 0) return infoWithCommentsDTOS;
        for (int i = 0; i < infos.size(); i++) {
            // 获取每条帖子对应的评论
            List<CommentInfo> commentInfos = commentService.selectByInfoId(infos.get(i).getId(),start,end);
            infoWithCommentsDTOS.add(new InfoWithCommentsDTO(infos.get(i),commentInfos));
        }
        return infoWithCommentsDTOS;
    }

    @Override
    public String generateUniqueId(Info info) {
        if(info == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        // 将帖子信息转换成字符串
        String infoString = info.toString();
        try {
            // 创建 MessageDigest 对象，指定使用 SHA-256 算法进行哈希计算
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            // 对帖子进行哈希计算
            byte[] hashBytes = digest.digest(infoString.getBytes());
            // 使用 Base64 编码将哈希结果转换成字符串
            String hashString = Base64.getEncoder().encodeToString(hashBytes);
            // 将用户名与哈希字符串拼接起来作为唯一标识符
            return info.getUserId()+ "-" + hashString;
        } catch (NoSuchAlgorithmException e) {
            // 哈希算法不支持时抛出异常
            throw new BusinessException(ErrorCode.SYSTEM_ERROR,"无法生成唯一Id");
        }
    }

    @Override
    public void cover(Info info) {
        if(info == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        infoMapper.cover(info);
    }

    /**
     * 发帖子
     *
     * @param info 帖子
     * @return
     * @throws IOException
     */
    @Async("threadPoolExecutor")
    @Override
    public Integer add(Info info) {
        if (info == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        int result = infoMapper.addInfo(info);
        // 此时 info 对象的 id 字段已经被设置为生成的主键值
        return result;
    }

    @Async("threadPoolExecutor")
    @Override
    public Future<Integer> updateById(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return new AsyncResult<>(infoMapper.updateById(id));
    }

    @Async("threadPoolExecutor")
    @Override
    public Future<Integer> decreaseUpdateById(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return new AsyncResult<>(infoMapper.decreaseUpdateById(id));
    }

    @Override
    public Info selectById(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return infoMapper.selectById(id);
    }

    @Override
    public List<Info> selectInfos(Integer userId) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return infoMapper.selectInfos(userId);
    }

    @Async("threadPoolExecutor")
    @Override
    public Future<Integer> deleteInfoWithComments(Integer id) {
        if (id == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        int i = infoMapper.deleteWithComments(id);
        return new AsyncResult<>(i == 0 ? 0 : 1);
    }

    @Override
    public Future<Integer> updateComments(Integer pubId) {
        if (pubId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return new AsyncResult<>(infoMapper.updateComments(pubId));
    }





    @Override
    public List<Info> getInfoListByIds(Set<String> infoIdList) {
        return infoMapper.selectByIds(infoIdList);
    }

    @Override
    public List<Info> selectInfoByPage(Integer userId, int start, int end) {
        if (userId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return infoMapper.selectByPage(userId, start, end);
    }


    @Override
    public List<InfoWithCommentsDTO> getDtoByPage(int page, int size) {
        //TODO
        int start = (page - 1) * size;
        int end = start + size - 1;
        //先读本地缓存，没有再去访问redis，redis有就写入本地缓存，没有再访问数据库，写入所有缓存。
        List<InfoWithCommentsDTO> result = new ArrayList<>(size);
        ConcurrentMap<@NonNull String, @NonNull String> map = caffeineCache.asMap();
        if(!map.isEmpty()){
            List<Map.Entry<@NonNull String, @NonNull String>> collect = map.entrySet().stream().limit(size).collect(Collectors.toList());
            for (Map.Entry<@NonNull String, @NonNull String> entry : collect) {
                Info info = JSON.parseObject(entry.getValue(), Info.class);
                List<CommentInfo> comments = commentService.selectPage(info.getId(), 1);
                result.add(new InfoWithCommentsDTO(info,comments));
            }
            if(result.size() < size){
                int need = size - result.size();
            }
        }

        return result;
    }

    @Override
    public InfoWithCommentsDTO getDtoById(Integer infoId) {
        InfoWithCommentsDTO result = null;
        //先去本地缓存
        String jsonInfo = caffeineCache.getIfPresent(String.valueOf(infoId));
        if(jsonInfo != null){
            Info info = JSON.parseObject(jsonInfo, Info.class);
            List<CommentInfo> comments = commentService.selectPage(infoId, 1);
            result = new InfoWithCommentsDTO(info,comments);
        }else{
            //没有再去访问redis
            jsonInfo = redisService.getInfoById(infoId);
            if(jsonInfo != null){
                //redis有就写入本地缓存
                Info info = JSON.parseObject(jsonInfo, Info.class);
                List<CommentInfo> comments = commentService.selectPage(infoId, 1);
                result = new InfoWithCommentsDTO(info,comments);
                caffeineCache.put(String.valueOf(infoId), jsonInfo);
            }else{
                //redis没有就去数据库，写入所有缓存。
                Info info = infoMapper.selectById(infoId);
                List<CommentInfo> comments = commentService.selectPage(infoId, 1);
                result = new InfoWithCommentsDTO(info,comments);
                redisService.setInfoById(infoId, JSON.toJSONString(info));
                caffeineCache.put(String.valueOf(infoId), JSON.toJSONString(info));
            }
        }
        return result;
    }
}

package com.hgc.campusechohotpostsservice.controller;

import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.constant.HotPostsConstant;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechohotpostsservice.service.RedisService;
import com.hgc.campusechohotpostsservice.uitl.ZsetKeyUtil;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.HotPostsService;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Info;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
@RestController
@RequestMapping("/hotPosts")
public class HotPostsController {
    @DubboReference
    private HotPostsService hotPostsService;
    @DubboReference
    private CommentService commentService;

    /**
     * 获取小时热门帖子
     * @param page 页码
     * @param size 页大小
     * @return
     */
    @GetMapping("/hour")
    public List<InfoWithCommentsDTO> getHourHotPosts(@RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size) {
        String key = ZsetKeyUtil.getKey(HotPostsConstant.HOUR);
        List<InfoWithCommentsDTO> result = new ArrayList<>(size);
        long start = (long) (page - 1) * size;
        long end = (long) page + size - 1;
        //获取热门帖子榜单
        List<Info> infoList = hotPostsService.getHotPosts(key, start, end);
        //调用评论服务获取相关评论
        for (Info info : infoList) {
            List<CommentInfo> commentInfoList = commentService.selectPage(info.getId(), 1);
            result.add(new InfoWithCommentsDTO(info, commentInfoList));
        }
        return result;
    }

    /**
     * 获取天热门帖子
     * @param page 页码
     * @param size 页大小
     * @return
     */
    @GetMapping("/day")
    public List<InfoWithCommentsDTO> getDayHotPosts(@RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size) {
        String key = ZsetKeyUtil.getKey(HotPostsConstant.DAY);
        List<InfoWithCommentsDTO> result = new ArrayList<>(size);
        long start = (long) (page - 1) * size;
        long end = (long) page + size - 1;
        //获取热门帖子榜单
        List<Info> infoList = hotPostsService.getHotPosts(key, start, end);
        //调用评论服务获取相关评论
        for (Info info : infoList) {
            List<CommentInfo> commentInfoList = commentService.selectPage(info.getId(), 1);
            result.add(new InfoWithCommentsDTO(info, commentInfoList));
        }
        return result;
    }

    /**
     * 获取周热门帖子
     * @param page 页码
     * @param size 页大小
     * @return
     */
    @GetMapping("/week")
    public List<InfoWithCommentsDTO> getWeekHotPosts(@RequestParam(value = "page", defaultValue = "1") Integer page, @RequestParam(value = "size", defaultValue = "10") Integer size) {
        String key = ZsetKeyUtil.getKey(HotPostsConstant.WEEK);
        List<InfoWithCommentsDTO> result = new ArrayList<>(size);
        long start = (long) (page - 1) * size;
        long end = (long) page + size - 1;
        //获取热门帖子榜单
        List<Info> infoList = hotPostsService.getHotPosts(key, start, end);
        //调用评论服务获取相关评论
        for (Info info : infoList) {
            List<CommentInfo> commentInfoList = commentService.selectPage(info.getId(), 1);
            result.add(new InfoWithCommentsDTO(info, commentInfoList));
        }
        return result;
    }

    /**
     * 更新热帖榜单
     * @param infoId
     * @return
     */
    @GetMapping("/update/{infoId}")
    public String updateHotPosts(@PathVariable("infoId") Integer infoId) {
        if(infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return hotPostsService.updateHotPosts(infoId);
    }

    /**
     * 将帖子信息从热帖榜单删除
     * @param infoId
     */
    @DeleteMapping("/delete/{infoId}")
    public String deleteHotPosts(@PathVariable("infoId") Integer infoId) {
        if (infoId == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        return hotPostsService.deleteHotPosts(infoId);
    }
}

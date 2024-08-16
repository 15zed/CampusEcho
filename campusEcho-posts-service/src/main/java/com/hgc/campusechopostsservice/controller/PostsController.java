package com.hgc.campusechopostsservice.controller;

import com.alibaba.fastjson.JSON;
import com.hgc.campusechocommon.common.ErrorCode;
import com.hgc.campusechocommon.exception.BusinessException;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.InfoService;
import com.hgc.campusechointerfaces.service.UserService;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechopostsservice.service.RedisService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 *
 */
@RestController
@RequestMapping("/posts")
public class PostsController {
    @Autowired
    private RedisService redisService;
    @Autowired
    private InfoService infoService;
    @DubboReference
    private UserService userService;
    @DubboReference
    private CommentService commentService;
    @Autowired
    private RabbitTemplate rabbitTemplate;
    @Autowired
    private TransactionTemplate transactionTemplate;


    /**
     * 发帖子
     * ✔
     *
     * @param info
     * @return
     */
    @PostMapping("/post")
    public String addInfo(@RequestBody Info info) {
        if (info == null) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        String uniqueId = infoService.generateUniqueId(info);
        if (!redisService.setUniqueId(uniqueId)) {
            return "请勿重复提交";
        }
        //MQ发送消息，本地MySQL和ES和多级缓存消费,发帖的场景一般没有太高的并发，所以不需要做高并发处理，这里使用MQ主要是因为ES和缓存需要同步数据，也可以使用canal
        rabbitTemplate.convertAndSend("addposts-fanoutExchange", "", info);
        return JSON.toJSONString(info);
    }


    /**
     * 删除帖子
     * ✔
     *
     * @param id 帖子id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public String deleteInfoWithComments(@PathVariable("id") Integer id) {
        if (id == null || id.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        List<Integer> commentIdList = commentService.selectIdsByInfoId(id);
        List<Object> list = new ArrayList<>();
        list.add(id);
        list.add(commentIdList);
        //发消息到MQ，本地MySQL和ES消费,计数服务消费，评论服务消费，多级缓存消费,榜单服务消费
        rabbitTemplate.convertAndSend("delposts-fanoutExchange", "", list);
        return "ok";
    }

//    /**
//     * 获取帖子和相关评论分页展示
//     *
//     * @return
//     */
//    @RequestMapping("/getdata")
//    public String getData(@RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) {
//        List<InfoWithCommentsDTO> dtoList = infoService.getDtoByPage(page, size);
//        return JSON.toJSONString(dtoList);
//    }

    @GetMapping("/getinfo/{infoId}")
    public String getInfoAndComments(@PathVariable("infoId") Integer infoId) {
        if (infoId == null || infoId.equals(0)) throw new BusinessException(ErrorCode.PARAMS_ERROR);
        InfoWithCommentsDTO dto = infoService.getDtoById(infoId);
        return JSON.toJSONString(dto);
    }


}

package com.hgc.school.controller;

import com.alibaba.fastjson.JSON;

import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.service.CommentService;
import com.hgc.school.service.ESService;
import com.hgc.school.service.InfoService;
import com.hgc.school.service.UserService;
import com.hgc.school.task.HotPostsTask;
import com.hgc.school.task.RecommendPostsTask;
import com.hgc.school.utils.ESIndexUtil;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 *
 */
@RestController
@RequestMapping("/api")
public class APIController {
    @Autowired
    private RecommendPostsTask recommendPostsTask;
    @Autowired
    private ESService esService;
    @Autowired
    private JedisPool jedisPool;
    @Autowired
    private InfoService infoService;
    @Autowired
    private UserService userService;
    @Autowired
    private CommentService commentService;
    @Autowired
    private HotPostsTask hotPostsTask;
    @Value("${school.filePrefix}")
    private String prefix;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @RequestMapping("/welcome")
    public ModelAndView toWelcome(){
        ModelAndView modelAndView = new ModelAndView();
        modelAndView.setViewName("welcome");
        return modelAndView;
    }

    /**
     * 获取所有数据 帖子和评论
     * @return
     */
    @RequestMapping("/getdata")
    public String getData() {
        //如果从redis获取全部数据，那用户添加评论的时候，就要把评论也要放到redis中，要保证redis中的评论数据和mysql中完全一致

//        List<Info> list = infoService.getData();
//        List<InfoWithCommentsDTO> result = new ArrayList<>();
//        for (Info info : list) {
//            List<CommentInfo> comments = commentService.selectByInfoId(info.getId());
//            InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
//            result.add(dto);
//        }
//        return JSON.toJSONString(result);

        List<Integer> infoIdList = infoService.selectAllId();
        List<Info> list = new ArrayList<>();

        List<InfoWithCommentsDTO> result = new ArrayList<>();
        Jedis jedis = jedisPool.getResource();
        for (Integer infoId : infoIdList) {
            String jsInfo = jedis.get("info:" + infoId);
            list.add(JSON.parseObject(jsInfo,Info.class));
        }
        for (Info info : list) {
            List<String> commentIdList = jedis.lrange("comment:pubId:" + info.getId(), 0, -1);
            List<CommentInfo> comments = new ArrayList<>();
            for (String commentId : commentIdList) {
                String jsComment = jedis.get("comment:" + commentId);
                comments.add(JSON.parseObject(jsComment,CommentInfo.class));
            }
            InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
            result.add(dto);
        }
        jedis.close();
        return JSON.toJSONString(result);
    }

    /**
     * 添加关注
     * @param userId 被关注人
     * @param session 获取关注人
     * @return
     */
    @PutMapping("/follow/{userId}")
    public String follow(@PathVariable Integer userId, HttpSession session){
        String jsonUser = (String) session.getAttribute("user");
        User user = JSON.parseObject(jsonUser, User.class);


        if(userService.addFollow(user.getUserId(),userId)){
            // 更新关注列表
            user.setFollows(userService.getFollows(user.getUserId()));
            //更新缓存
            Jedis jedis = jedisPool.getResource();
            jedis.set("user:"+user.getUserId(),JSON.toJSONString(user));
            jedis.set("user:"+userId,JSON.toJSONString(userService.selectById(userId)));

            // 更新session中的用户信息
            session.setAttribute("user", JSON.toJSONString(user));
            jedis.close();
            return "关注成功";
        }
        else
            return "关注失败";
    }

    /**
     * 获取所有关注
     * @param session
     * @return
     */
    @GetMapping("/follow/getFollows")
    public ModelAndView getFollows(HttpSession session){
//        ModelAndView modelAndView = new ModelAndView();
//        String jsonUser = (String) session.getAttribute("user");
//        User user = JSON.parseObject(jsonUser, User.class);
//        List<User> followList= userService.selectFollows(user.getUserId());
//        modelAndView.addObject("followList",followList);
//        modelAndView.setViewName("myFollow");
//        return modelAndView;

        ModelAndView modelAndView = new ModelAndView();
        Jedis jedis = jedisPool.getResource();
        String jsonUser = (String) session.getAttribute("user");
        User user = JSON.parseObject(jsonUser, User.class);
        //这里用户的信息在缓存中肯定是有的
        String jsUser = jedis.get("user:" + user.getUserId());
        List<User> followList = new ArrayList<>();

        User user1 = JSON.parseObject(jsUser, User.class);
        String follows = user1.getFollows();
        if (follows != null && !follows.isEmpty()) {
            String[] followArr = follows.split(",");
            for (String userId : followArr) {
                String strUser = jedis.get("user:" + userId);
                followList.add(JSON.parseObject(strUser, User.class));
            }
        }
        modelAndView.addObject("followList", followList);
        modelAndView.setViewName("myFollow");
        jedis.close();
        return modelAndView;
    }

    /**
     * 取消关注
     * @param userId 取消关注谁
     * @param session 获取取消人
     * @return
     */
    @PutMapping("/unfollow/{userId}")
    public String unfollow(@PathVariable Integer userId,HttpSession session){
        String jsonUser = (String) session.getAttribute("user");
        User user = JSON.parseObject(jsonUser, User.class);
        if(userService.unFollow(user.getUserId(),userId)) {
            // 更新关注列表
            user.setFollows(userService.getFollows(user.getUserId()));
            //更新缓存
            Jedis jedis = jedisPool.getResource();
            jedis.set("user:"+user.getUserId(),JSON.toJSONString(user));
            jedis.set("user:"+userId,JSON.toJSONString(userService.selectById(userId)));

            // 更新session中的用户信息
            session.setAttribute("user", JSON.toJSONString(user));
            jedis.close();
            return "取消成功";
        }
        else
            return "取消失败";
    }

    /**
     * 获取所有粉丝
     * @param session
     * @return
     */
    @GetMapping("/fans/getFans")
    public ModelAndView getFans(HttpSession session){
//        ModelAndView modelAndView = new ModelAndView();
//        String jsonUser = (String) session.getAttribute("user");
//        User user = JSON.parseObject(jsonUser, User.class);
//        List<User> fansList = userService.selectFans(user.getUserId());
//        user.setFans(userService.getFans(user.getUserId()));
//        modelAndView.addObject("fansList",fansList);
//        modelAndView.addObject("userObj",user);
//        modelAndView.setViewName("myFans");
//        return modelAndView;

        ModelAndView modelAndView = new ModelAndView();
        Jedis jedis = jedisPool.getResource();
        String jsonUser = (String) session.getAttribute("user");
        User user = JSON.parseObject(jsonUser, User.class);
        //这里用户的信息在缓存中肯定是有的
        String jsUser = jedis.get("user:" + user.getUserId());
        List<User> fansList = new ArrayList<>();
        User user1 = JSON.parseObject(jsUser, User.class);
        String fans = user1.getFans();
        user.setFans(fans);
        if(fans != null && !fans.isEmpty()){
            String[] fansArr = fans.split(",");
            for (String userId : fansArr) {
                String strUser = jedis.get("user:" + userId);
                fansList.add(JSON.parseObject(strUser,User.class));
            }
        }
        modelAndView.addObject("fansList",fansList);
        modelAndView.addObject("userObj",user);
        modelAndView.setViewName("myFans");
        jedis.close();
        return modelAndView;
    }


    /**
     * 上传图片
     * @param file
     * @return
     * @throws IOException
     */
    @PostMapping("/uploadImage")
    public String upload(@RequestParam("image")MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        //截取原始文件名后缀 .jpg  .png  .....
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        //拼接成最后的文件名
        String filename = UUID.randomUUID() + suffix;
        file.transferTo(new File(prefix + filename));
        return JSON.toJSONString(filename);
    }

    /**
     * 发帖子
     * @param info
     * @return
     */
    @PostMapping("/post")
    public String addInfo(@RequestBody Info info) throws IOException {
        //这里设置了会把生成的主键id赋值给info对象的id字段 返回info的所有信息 不然前端的有些操作会拿不到帖子的id
        infoService.add(info);
        //更新缓存
        Jedis jedis = jedisPool.getResource();
        jedis.set("info:"+info.getId(),JSON.toJSONString(info));
        jedis.lpush("info:userId:"+info.getUserId(), String.valueOf(info.getId()));
        jedis.expire("info:"+info.getId(),60*60*24);
        jedis.close();
        //更新ES
        IndexRequest request = new IndexRequest(ESIndexUtil.INFO_INDEX);
        request.id(String.valueOf(info.getId()));
        request.source(JSON.toJSONString(info), XContentType.JSON);
        restHighLevelClient.index(request,RequestOptions.DEFAULT);
        return JSON.toJSONString(info);
    }

    /**
     * 发评论
     * @param comment
     * @return
     */
    @PostMapping("/comment")
    public String addComment(@RequestBody CommentInfo comment) throws IOException {
        //设置主键生成之后自动往commentInfo对象的commentId字段加
        commentService.add(comment);
        infoService.updateComments(comment.getPubId());
        //更新缓存
        Jedis jedis = jedisPool.getResource();
        jedis.set("comment:"+ comment.getCommentId(),JSON.toJSONString(comment));
        jedis.expire("comment:"+comment.getCommentId(),60*60*24);
        jedis.lpush("comment:pubId:"+comment.getPubId(), String.valueOf(comment.getCommentId()));
        jedis.set("info:"+infoService.selectById(comment.getPubId()).getId(),JSON.toJSONString(infoService.selectById(comment.getPubId())));
        jedis.close();
        //更新ES
        IndexRequest request = new IndexRequest(ESIndexUtil.COMMENT_INDEX);
        request.id(String.valueOf(comment.getCommentId()));
        request.source(JSON.toJSONString(comment), XContentType.JSON);
        restHighLevelClient.index(request,RequestOptions.DEFAULT);

        UpdateRequest updateRequest = new UpdateRequest(ESIndexUtil.INFO_INDEX, String.valueOf(comment.getPubId()));
        Integer comments = infoService.selectById(comment.getPubId()).getComments();
        Map<String,Object> doc = new HashMap<>();
        doc.put("comments",comments);
        updateRequest.doc(doc,XContentType.JSON);
        restHighLevelClient.update(updateRequest,RequestOptions.DEFAULT);
        return JSON.toJSONString(comment);
    }


    /**
     * 点赞
     * @param id
     * @param request
     * @return
     */
    @PostMapping("/{id}/like")
    public ResponseEntity<?> addLikes(@PathVariable("id") Integer id,HttpServletRequest request){
        String jsonUser = (String) request.getSession().getAttribute("user");
        User user = JSON.parseObject(jsonUser, User.class);

        try {
            // 检查用户是否已经点赞过该帖子
            if (userService.hasUserLikedPost(user.getUserId(), id)) {
                return ResponseEntity
                        .badRequest()
                        .body("用户已经点赞过该帖子。");
            }
            // 更新用户的点赞记录
            userService.addPostToUserLikes(user.getUserId(), id);
            Jedis jedis = jedisPool.getResource();
            jedis.set("user:"+user.getUserId(),JSON.toJSONString(userService.selectById(user.getUserId())));
            jedis.set("user:"+user.getUsername(), String.valueOf(user.getUserId()));
            //更新帖子的点赞数
            infoService.updateById(id);


            // 返回更新后的帖子数据
            Info info = infoService.selectById(id);
            jedis.set("info:"+info.getId(),JSON.toJSONString(info));
            jedis.close();

            Integer likes = info.getLikes();
            //更新ES
            UpdateRequest updateRequest = new UpdateRequest(ESIndexUtil.INFO_INDEX, String.valueOf(id));
            // 构建部分更新的文档
            Map<String, Object> doc = new HashMap<>();
            doc.put("likes", likes);
            // 使用doc方法进行部分更新
            updateRequest.doc(doc, XContentType.JSON);
            // 执行更新请求
            restHighLevelClient.update(updateRequest, RequestOptions.DEFAULT);

            return ResponseEntity
                    .ok()
                    .body(info);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("服务器错误。");
        }
    }

    /**
     * 删除帖子和评论
     * @param id
     * @return
     */
    @DeleteMapping("/delete/{id}")
    public String deleteInfoWithComments(@PathVariable("id")Integer id, HttpServletRequest request) throws IOException {
        BulkRequest bulkRequest = new BulkRequest();
        Info info = infoService.selectById(id);
        infoService.deleteInfoWithComments(id);
        //更新缓存
        Jedis jedis = jedisPool.getResource();
        jedis.del("info:"+id);
        jedis.lrem("info:userId:"+info.getUserId(),0, String.valueOf(info.getId()));
        List<String> commentIdList = jedis.lrange("comment:pubId:" + info.getId(), 0, -1);
        for (String commentId : commentIdList) {
            jedis.del("comment:"+commentId);
            //更新ES 删除所有评论
            DeleteRequest deleteRequest = new DeleteRequest(ESIndexUtil.COMMENT_INDEX, commentId);
            bulkRequest.add(deleteRequest);
        }
        jedis.del("comment:pubId:"+info.getId());
        jedis.close();
        //更新ES 删除帖子
        DeleteRequest deleteRequest = new DeleteRequest(ESIndexUtil.INFO_INDEX, String.valueOf(id));
        restHighLevelClient.delete(deleteRequest,RequestOptions.DEFAULT);
        restHighLevelClient.bulk(bulkRequest,RequestOptions.DEFAULT);

        String jsonUser = (String) request.getSession().getAttribute("user");
        User user = JSON.parseObject(jsonUser, User.class);
        return "redirect:/user/"+user.getUserId();
    }

    /**
     * 获取热点帖子数据
     *
     * @return
     */
    @RequestMapping("/hotPosts")
    public String getHotPosts(){
       //调用定时任务返回热点帖子数据，转成json给前端
        List<InfoWithCommentsDTO> hotPosts = hotPostsTask.getHotPosts();
        return JSON.toJSONString(hotPosts);
    }

    /**
     * ES查询帖子
     * @param keyword
     * @return
     */
    @GetMapping("/search/{keyword}")
    public String searchPost(@PathVariable String keyword){
        List<InfoWithCommentsDTO> dtoList = esService.search(keyword);
        return JSON.toJSONString(dtoList);
    }

    /**
     * 推荐帖子
     *
     * @return
     */
    @GetMapping("/recommendPosts")
    public String getRecommendPosts(HttpSession session){
        String jsonUser = (String) session.getAttribute("user");
        User user = JSON.parseObject(jsonUser, User.class);
        //调用定时任务返回推荐帖子数据，转成json给前端
        List<InfoWithCommentsDTO> recommendPosts = recommendPostsTask.getRecommendPosts(user.getUserId());
        return JSON.toJSONString(recommendPosts);
    }
}

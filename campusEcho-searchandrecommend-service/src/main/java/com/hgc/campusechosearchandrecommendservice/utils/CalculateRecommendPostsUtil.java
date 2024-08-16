package com.hgc.campusechosearchandrecommendservice.utils;

import com.hgc.campusechocommon.constant.HotPostsConstant;
import com.hgc.campusechointerfaces.service.CommentService;
import com.hgc.campusechointerfaces.service.HotPostsService;
import com.hgc.campusechomodel.dto.InfoWithCommentsDTO;
import com.hgc.campusechomodel.entity.CommentInfo;
import com.hgc.campusechomodel.entity.Info;
import com.hgc.campusechomodel.entity.User;
import com.hgc.campusechosearchandrecommendservice.mapper.InfoMapper;
import com.hgc.campusechosearchandrecommendservice.mapper.LikesMapper;
import com.hgc.campusechosearchandrecommendservice.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * 计算推荐帖子工具类
 */
@Component
@Slf4j
public class CalculateRecommendPostsUtil {
    @DubboReference
    private CommentService commentService;
    @DubboReference
    private HotPostsService hotPostsService;


    @Autowired
    private UserMapper userMapper;
    @Autowired
    private InfoMapper infoMapper;
    @Autowired
    private LikesMapper likesMapper;

    public Map<Integer, List<InfoWithCommentsDTO>> calculate() {
        Map<Integer, List<InfoWithCommentsDTO>> resultMap = new HashMap<>();
        List<Integer> userIdList = userMapper.selectIds();
        List<Integer> infoIdList = infoMapper.selectIds();
        log.info("infoIdList最初大小：" + infoIdList.size());
        //遍历所有用户id
        for (Integer userId : userIdList) {
            User user = userMapper.selectById(userId);
            List<Integer> likeList = likesMapper.getUserLikeList(user.getUserId());//拿到用户的点赞帖子列表
            //如果点赞帖子列表不够或者点赞帖子为空，用热点帖子，下一次循环
            if (likeList == null || likeList.isEmpty() || likeList.size() < 10) {
                LocalDateTime now = LocalDateTime.now();// 获取当前时间
                int dayOfYear = now.getDayOfYear();// 获取当前时间是一年的第多少天(1~365，1~366)
                String suffix = HotPostsConstant.DAY + dayOfYear;
                List<Info> hotPosts = hotPostsService.getHotPosts(HotPostsConstant.DAY + suffix, 0, 15);
                //调用评论服务获取相关评论
                List<InfoWithCommentsDTO> temp = new ArrayList<>();
                for (Info info : hotPosts) {
                    List<CommentInfo> comments = commentService.selectPage(info.getId(), 1);
                    temp.add(new InfoWithCommentsDTO(info, comments));
                }
                resultMap.put(userId, temp);
                continue;
            }
            int rowLength = likeList.size(); //矩阵的行数 = 用户点赞的帖子数
            Set<String> likeCategorySet = new HashSet<>();//用户喜欢的帖子种类 使用set去重
            //遍历用户喜欢的帖子id，从数据库所有帖子中去除掉
            for (Integer infoId : likeList) {
                infoIdList.remove(infoId);//所有帖子列表中 去除掉该用户喜欢的帖子
                String category = infoMapper.selectById(infoId).getCategory(); //获取该用户喜欢的帖子种类
                likeCategorySet.add(category);//加入到set中去重
            }
            //再从剩余的所有帖子id中去掉所有和用户喜欢的类型不符的帖子id
            Iterator<Integer> iterator = infoIdList.iterator();
            while (iterator.hasNext()) {
                Integer infoId = iterator.next();
                Info info = infoMapper.selectById(infoId);
                if (!likeCategorySet.contains(info.getCategory())) {
                    iterator.remove();
                }
            }
            int colLength = infoIdList.size();//矩阵的列数 = 数据库符合要求的帖子数
            log.info("infoIdList去掉以后的大小：" + colLength);
            //构建相似矩阵
            RealMatrix matrix = MatrixUtils.createRealMatrix(rowLength, colLength);
            // 遍历矩阵并计算相似度
            for (int i = 0; i < matrix.getRowDimension(); i++) {
                for (int j = 0; j < matrix.getColumnDimension(); j++) {
                    Integer rowId = likeList.get(i);//i位置：用户喜欢的帖子id
                    Integer colId = infoIdList.get(j);//j位置：数据库符合要求的帖子id
                    List<User> rowUserList = new ArrayList<>(1000);
                    List<User> colUserList = new ArrayList<>(1000);
                    //遍历所有用户id,排除自己
                    userIdList.remove(user.getUserId());
                    for (Integer uId : userIdList) {
                        List<Integer> temUserLikelist = likesMapper.getUserLikeList(uId);
                        User temUser = userMapper.selectById(uId);
                        // 把所有点赞过i位置帖子的用户加入到集合中
                        if (temUserLikelist != null && !temUserLikelist.isEmpty() && temUserLikelist.contains(rowId)) {
                            rowUserList.add(temUser);
                        }
                        // 把所有点赞过j位置帖子的用户加入到集合中
                        if (temUserLikelist != null && !temUserLikelist.isEmpty() && temUserLikelist.contains(colId)) {
                            colUserList.add(temUser);
                        }
                    }
                    // 计算相似度
                    double similarity = calculateJaccardSimilarity(rowUserList, colUserList, rowId, colId);
                    // 设置相似度到矩阵的[i, j]位置
                    matrix.setEntry(i, j, similarity);
                }
            }
            // 计算每一列的平均值，代表矩阵j位置帖子(符合要求的，待推荐的帖子)和用户喜欢的所有帖子之间的平均相似度
            double[] columnAverages = new double[colLength];
            for (int j = 0; j < colLength; j++) {
                double columnSum = 0.0;
                for (int i = 0; i < rowLength; i++) {
                    columnSum += matrix.getEntry(i, j);
                }
                columnAverages[j] = columnSum / rowLength;
            }
            //key是 数据库符合条件的帖子的id value是对应帖子和当前用户点赞过的所有帖子的平均相似度
            //相似度可能相同 不能放在key位置 不然会同名覆盖掉
            TreeMap<Integer, Double> treeMap = new TreeMap<>();
            for (int i = 0; i < colLength; i++) {
                treeMap.put(infoIdList.get(i), columnAverages[i]);
            }
            //排序 根据value(相似度)从大到小排序
            TreeMap<Integer, Double> newMap = treeMap.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .collect(
                            TreeMap::new,
                            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
                            TreeMap::putAll
                    );
            List<InfoWithCommentsDTO> dtoList = new ArrayList<>();//准备一个集合存储所有的dto dto存的就是推荐的帖子
            //遍历排序后的map
            for (Map.Entry<Integer, Double> entry : newMap.entrySet()) {
                if (entry.getValue() > 0) {//如果相似度大于0 ,这里可以修改，相似度范围是(0,1)之间，由于这里数据较少，所以设了一个相似度较低的值
                    Integer infoId = entry.getKey();
                    Info info = infoMapper.selectById(infoId);
                    List<CommentInfo> comments = commentService.selectPage(info.getId(), 1);
                    InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
                    if (dtoList.size() <= 16) dtoList.add(dto);//相似度>0 ,集合容量<16 就放里面
                }
            }
            int min = Math.min(dtoList.size(), 16); //min 取值范围【0-16】 不够就添加帖子，凑够16条推荐帖子
            int stillNeedNumber = (min == 16 ? 0 : 16 - min);//还需要多少条帖子
            if (stillNeedNumber != 0) {
                //从热门帖子里面添加
                LocalDateTime now = LocalDateTime.now();// 获取当前时间
                int dayOfYear = now.getDayOfYear();// 获取当前时间是一年的第多少天(1~365，1~366)
                String suffix = HotPostsConstant.DAY + dayOfYear;
                List<Info> hotPosts = hotPostsService.getHotPosts(HotPostsConstant.DAY + suffix, 0, stillNeedNumber - 1);
                for (Info info : hotPosts) {
                    List<CommentInfo> comments = commentService.selectPage(info.getId(), 1);
                    dtoList.add(new InfoWithCommentsDTO(info, comments));
                }
            }
            resultMap.put(userId, dtoList);//最后把该用户推荐帖子的集合放到map里
        }
        return resultMap;
    }

    /**
     * 计算相似度
     *
     * @param rowUserList 当前行位置 所有点赞过该帖子的用户集合
     * @param colUserList 当前列位置 所有点赞过该帖子的用户集合
     * @param rowId       当前行位置的帖子id
     * @param colId       当前列位置的帖子id
     * @return
     */
    private double calculateJaccardSimilarity(List<User> rowUserList, List<User> colUserList, Integer rowId, Integer colId) {
        //如果两个集合为空 直接返回0
        if (colUserList.isEmpty() || rowUserList.isEmpty()) return 0.0;
        Set<Integer> intersection = new HashSet<>();//交集: 里面存放的是i位置和j位置都点赞过的用户的id
        Set<Integer> union = new HashSet<>();//并集: 将集合A和集合B中所有的元素合并到一个新的集合中，且不重复地包含所有元素。
        //遍历所有点赞过i 位置帖子的用户
        for (User rowUser : rowUserList) {
            union.add(rowUser.getUserId());//直接加入并集 自动去重
            if (likesMapper.getUserLikeList(rowUser.getUserId()).contains(colId)) {
                intersection.add(rowUser.getUserId());//如果该用户还点赞过j位置的帖子，加入交集
            }
        }
        //遍历所有点赞过j 位置帖子的用户
        for (User colUser : colUserList) {
            union.add(colUser.getUserId());//直接加入并集 自动去重
            if (likesMapper.getUserLikeList(colUser.getUserId()).contains(rowId)) {
                intersection.add(colUser.getUserId());//如果该用户还点赞过i位置的帖子，加入交集
            }
        }
        int unionSize = union.size();//并集的大小：不可能为0
        int intersectionSize = intersection.size();//交集的大小
        //如果交集为空 直接返回0 不用做除法运算
        if (intersectionSize == 0) {
            return 0.0;
        }
        //Jaccard 相似度 = 两个集合的交集 / 两个集合的并集，广泛应用于信息检索、推荐系统、文本挖掘等领域，用于衡量数据项之间的相似度。
        return (double) intersectionSize / unionSize;//返回相似度，取值范围(0,1)
    }
}

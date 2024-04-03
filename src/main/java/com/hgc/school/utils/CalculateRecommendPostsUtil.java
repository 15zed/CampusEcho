//package com.hgc.school.utils;
//
//import com.alibaba.fastjson.JSON;
//import com.hgc.school.dto.InfoWithCommentsDTO;
//import com.hgc.school.mapper.InfoMapper;
//import com.hgc.school.mapper.UserMapper;
//import com.hgc.school.task.HotPostsTask;
//import com.hgc.school.vo.CommentInfo;
//import com.hgc.school.vo.Info;
//import com.hgc.school.vo.User;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.commons.math3.linear.MatrixUtils;
//import org.apache.commons.math3.linear.RealMatrix;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//import redis.clients.jedis.Jedis;
//import redis.clients.jedis.JedisPool;
//
//import java.util.*;
//
///**
// *
// */
//@Component
//@Slf4j
//public class CalculateRecommendPostsUtil {
//    @Autowired
//    private JedisPool jedisPool;
//    @Autowired
//    private UserMapper userMapper;
//    @Autowired
//    private HotPostsTask hotPostsTask;
//    @Autowired
//    private InfoMapper infoMapper;
//
//    public Map<Integer, List<InfoWithCommentsDTO>> calculate() {
//        Map<Integer, List<InfoWithCommentsDTO>> resultMap = new HashMap<>();
//        Jedis jedis = jedisPool.getResource();
//        List<Integer> userIdList = userMapper.selectIds();
//        List<Integer> infoIdList = infoMapper.selectIds();
//        log.info("infoIdList最初大小：" + infoIdList.size());
//        //遍历所有用户id
//        for (Integer userId : userIdList) {
//            User user = JSON.parseObject(jedis.get("user:" + userId), User.class);
//            String likelist = user.getLikelist();//拿到用户的点赞帖子列表
//            //如果点赞帖子列表不够或者点赞帖子为空，用热点帖子，下一次循环
//            if (likelist == null || likelist.isEmpty() || likelist.split(",").length <= 5) {
//                List<InfoWithCommentsDTO> hotPosts = hotPostsTask.getHotPosts();
//                resultMap.put(userId, hotPosts);
//                continue;
//            }
//            String[] likePostsIdArray = likelist.split(",");//用户喜欢的帖子列表
//            Set<String> likeCategorySet = new HashSet<>();//用户喜欢的帖子种类 使用set去重
//            int rowLength = likePostsIdArray.length;//矩阵的行数
////            double[] rowLikePostsIdArray = new double[likePostsIdArray.length];
//            for (int i = 0; i < likePostsIdArray.length; i++) {
////                rowLikePostsIdArray[i] = Double.parseDouble(likePostsIdArray[i]);
//                infoIdList.remove(Integer.valueOf(likePostsIdArray[i]));//所有帖子列表中 去除掉该用户喜欢的帖子
//                String category = JSON.parseObject(jedis.get("info:" + likePostsIdArray[i]), Info.class).getCategory();
//                likeCategorySet.add(category);
//            }
//            //再从剩余的所有帖子中去掉所有和用户喜欢的类型不符的帖子
//            Iterator<Integer> iterator = infoIdList.iterator();
//            while (iterator.hasNext()) {
//                Integer infoId = iterator.next();
//                Info info = JSON.parseObject(jedis.get("info:" + infoId), Info.class);
//                if (!likeCategorySet.contains(info.getCategory())) {
//                    iterator.remove();
//                }
//            }
//            int colLength = infoIdList.size();//矩阵的列数
//            log.info("infoIdList去掉以后的大小：" + colLength);
//            //构建矩阵
//            RealMatrix matrix = MatrixUtils.createRealMatrix(rowLength, colLength);
//            // 遍历矩阵并计算相似度
//            for (int i = 0; i < matrix.getRowDimension(); i++) {
//                for (int j = 0; j < matrix.getColumnDimension(); j++) {
//                    String rowId = likePostsIdArray[i];//i位置：用户喜欢的帖子id
//                    String colId = String.valueOf(infoIdList.get(j));//j位置：数据库符合要求的帖子id
//                    List<User> rowUserList = new ArrayList<>();
//                    List<User> colUserList = new ArrayList<>();
//                    //遍历所有用户
//                    for (Integer uId : userIdList) {
//                        User temUser = JSON.parseObject(jedis.get("user:" + uId), User.class);
//                        String temUserLikelist = temUser.getLikelist();
//                        // 把所有点赞过i位置帖子的用户加入到集合中
//                        if (temUserLikelist != null && !temUserLikelist.isEmpty() && temUserLikelist.contains(rowId)) {
//                            rowUserList.add(temUser);
//                        }
//                        // 把所有点赞过j位置帖子的用户加入到集合中
//                        if (temUserLikelist != null && !temUserLikelist.isEmpty() && temUserLikelist.contains(colId)) {
//                            colUserList.add(temUser);
//                        }
//                    }
//                    // 计算相似度
//                    double similarity = calculateJaccardSimilarity(rowUserList, colUserList, rowId, colId);
//                    // 设置相似度到矩阵的[i, j]位置
//                    matrix.setEntry(i, j, similarity);
//                }
//            }
//            // 计算每一列的平均值
//            double[] columnAverages = new double[colLength];
//            for (int j = 0; j < colLength; j++) {
//                double columnSum = 0.0;
//                for (int i = 0; i < rowLength; i++) {
//                    columnSum += matrix.getEntry(i, j);
//                }
//                columnAverages[j] = columnSum / rowLength;
//            }
//            //key是 数据库符合条件的帖子的id value是对应帖子和当前用户点赞过的所有帖子的平均相似度
//            //相似度可能相同 不能放在key位置 不然会同名覆盖掉
//            TreeMap<Integer, Double> treeMap = new TreeMap<>();
//            for (int i = 0; i < columnAverages.length; i++) {
//                treeMap.put(infoIdList.get(i), columnAverages[i]);
//            }
//            //排序 根据value(相似度)从大到小排序
//            TreeMap<Integer, Double> newMap = treeMap.entrySet().stream()
//                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
//                    .collect(
//                            TreeMap::new,
//                            (map, entry) -> map.put(entry.getKey(), entry.getValue()),
//                            TreeMap::putAll
//                    );
//            List<InfoWithCommentsDTO> dtoList = new ArrayList<>();//准备一个集合存储所有的dto dto存的就是推荐的帖子
//            //遍历排序后的map
//            for (Map.Entry<Integer, Double> entry : newMap.entrySet()) {
//                if (entry.getValue() > 0) {//如果相似度大于0
//                    Integer infoId = entry.getKey();
//                    Info info = JSON.parseObject(jedis.get("info:" + infoId), Info.class);
//                    List<String> commentIdList = jedis.lrange("comment:pubId:" + infoId, 0, -1);
//                    List<CommentInfo> comments = new ArrayList<>();
//                    for (String commentId : commentIdList) {
//                        CommentInfo commentInfo = JSON.parseObject(jedis.get("comment:" + commentId), CommentInfo.class);
//                        comments.add(commentInfo);
//                    }
//                    InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
//                    if (dtoList.size() <= 16) dtoList.add(dto);//相似度>0 ,集合容量<16 就放里面
//                }
//            }
//            int min = Math.min(dtoList.size(), 16); //min 取值范围【0-16】 不够就添加帖子，凑够16条推荐帖子
//            int stillNeedNumber = (min == 16 ? 0 : 16 - min);//还需要多少条帖子
//            if (stillNeedNumber != 0) {
//                int count = 0;
//                List<InfoWithCommentsDTO> hotPosts = hotPostsTask.getHotPosts();//从热门帖子里面添加
//                while (count < stillNeedNumber) {
//                    dtoList.add(hotPosts.get(count));
//                    count++;
//                }
//            }
//            resultMap.put(userId, dtoList);//最后把该用户推荐帖子的集合放到map里
//        }
//        return resultMap;
//    }
//
//    /**
//     * 计算相似度
//     * @param rowUserList 当前行位置 所有点赞过该帖子的用户集合 不可能为空 因为当前用户也被包含进来了
//     * @param colUserList 当前列位置 所有点赞过该帖子的用户集合 可能为空
//     * @param rowId 当前行位置的帖子id
//     * @param colId 当前列位置的帖子id
//     * @return
//     */
//    private double calculateJaccardSimilarity(List<User> rowUserList, List<User> colUserList, String rowId, String colId) {
//        //求交集和并集
//        if (colUserList.size() == 0) return 0.0;
//        Set<Integer> intersection = new HashSet<>();//交集
//        Set<Integer> union = new HashSet<>();//并集
//        //遍历所有点赞过i 位置帖子的用户
//        for (User rowUser : rowUserList) {
//            union.add(rowUser.getUserId());//直接加入并集 自动去重
//            if (rowUser.getLikelist().contains(colId))
//                intersection.add(rowUser.getUserId());//如果该用户还点赞过j位置的帖子，加入交集
//        }
//        //遍历所有点赞过j 位置帖子的用户
//        for (User colUser : colUserList) {
//            union.add(colUser.getUserId());//直接加入并集 自动去重
//            if (colUser.getLikelist().contains(rowId))
//                intersection.add(colUser.getUserId());//如果该用户还点赞过i位置的梯子，加入交集
//        }
//        int unionSize = union.size();//不可能为空
//        int intersectionSize = intersection.size();
//        //如果交集为空 直接返回0
//        if (intersectionSize == 0) {
//            return 0.0;
//        }
//        return (double) intersectionSize / unionSize;
//    }
//
//
//}

package com.hgc.school.utils;


import com.hgc.school.dto.InfoWithCommentsDTO;
import com.hgc.school.mapper.CommentMapper;
import com.hgc.school.mapper.InfoMapper;
import com.hgc.school.mapper.UserMapper;
import com.hgc.school.task.HotPostsTask;
import com.hgc.school.vo.CommentInfo;
import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.*;

/**
 *
 */
@Component
@Slf4j
public class CalculateRecommendPostsUtil {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private HotPostsTask hotPostsTask;
    @Autowired
    private InfoMapper infoMapper;
    @Autowired
    private CommentMapper commentMapper;

    public Map<Integer, List<InfoWithCommentsDTO>> calculate() {
        Map<Integer, List<InfoWithCommentsDTO>> resultMap = new HashMap<>();
        List<Integer> userIdList = userMapper.selectIds();
        List<Integer> infoIdList = infoMapper.selectIds();
        log.info("infoIdList最初大小：" + infoIdList.size());
        //遍历所有用户id
        for (Integer userId : userIdList) {
            User user = userMapper.selectById(userId);
            String likelist = user.getLikelist();//拿到用户的点赞帖子列表
            //如果点赞帖子列表不够或者点赞帖子为空，用热点帖子，下一次循环
            if (likelist == null || likelist.isEmpty() || likelist.split(",").length <= 5) {
                List<InfoWithCommentsDTO> hotPosts = hotPostsTask.getHotPosts();
                resultMap.put(userId, hotPosts);
                continue;
            }
            String[] likePostsIdArray = likelist.split(",");//用户喜欢的帖子列表
            Set<String> likeCategorySet = new HashSet<>();//用户喜欢的帖子种类 使用set去重
            int rowLength = likePostsIdArray.length;//矩阵的行数
            for (int i = 0; i < likePostsIdArray.length; i++) {
                infoIdList.remove(Integer.valueOf(likePostsIdArray[i]));//所有帖子列表中 去除掉该用户喜欢的帖子
                String category = infoMapper.selectById(Integer.valueOf(likePostsIdArray[i])).getCategory();
                likeCategorySet.add(category);
            }
            //再从剩余的所有帖子中去掉所有和用户喜欢的类型不符的帖子
            Iterator<Integer> iterator = infoIdList.iterator();
            while (iterator.hasNext()) {
                Integer infoId = iterator.next();
                Info info = infoMapper.selectById(infoId);
                if (!likeCategorySet.contains(info.getCategory())) {
                    iterator.remove();
                }
            }
            int colLength = infoIdList.size();//矩阵的列数
            log.info("infoIdList去掉以后的大小：" + colLength);
            //构建矩阵
            RealMatrix matrix = MatrixUtils.createRealMatrix(rowLength, colLength);
            // 遍历矩阵并计算相似度
            for (int i = 0; i < matrix.getRowDimension(); i++) {
                for (int j = 0; j < matrix.getColumnDimension(); j++) {
                    String rowId = likePostsIdArray[i];//i位置：用户喜欢的帖子id
                    String colId = String.valueOf(infoIdList.get(j));//j位置：数据库符合要求的帖子id
                    List<User> rowUserList = new ArrayList<>();
                    List<User> colUserList = new ArrayList<>();
                    //遍历所有用户
                    for (Integer uId : userIdList) {
                        User temUser = userMapper.selectById(uId);
                        String temUserLikelist = temUser.getLikelist();
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
            // 计算每一列的平均值
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
            for (int i = 0; i < columnAverages.length; i++) {
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
                    List<CommentInfo> comments = commentMapper.selectByInfoId(infoId);
                    InfoWithCommentsDTO dto = new InfoWithCommentsDTO(info, comments);
                    if (dtoList.size() <= 16) dtoList.add(dto);//相似度>0 ,集合容量<16 就放里面
                }
            }
            int min = Math.min(dtoList.size(), 16); //min 取值范围【0-16】 不够就添加帖子，凑够16条推荐帖子
            int stillNeedNumber = (min == 16 ? 0 : 16 - min);//还需要多少条帖子
            if (stillNeedNumber != 0) {
                int count = 0;
                List<InfoWithCommentsDTO> hotPosts = hotPostsTask.getHotPosts();//从热门帖子里面添加
                while (count < stillNeedNumber) {
                    dtoList.add(hotPosts.get(count));
                    count++;
                }
            }
            resultMap.put(userId, dtoList);//最后把该用户推荐帖子的集合放到map里
        }
        return resultMap;
    }

    /**
     * 计算相似度
     *
     * @param rowUserList 当前行位置 所有点赞过该帖子的用户集合 不可能为空 因为当前用户也被包含进来了
     * @param colUserList 当前列位置 所有点赞过该帖子的用户集合 可能为空
     * @param rowId       当前行位置的帖子id
     * @param colId       当前列位置的帖子id
     * @return
     */
    private double calculateJaccardSimilarity(List<User> rowUserList, List<User> colUserList, String rowId, String colId) {
        //求交集和并集
        if (colUserList.size() == 0) return 0.0;
        Set<Integer> intersection = new HashSet<>();//交集
        Set<Integer> union = new HashSet<>();//并集
        //遍历所有点赞过i 位置帖子的用户
        for (User rowUser : rowUserList) {
            union.add(rowUser.getUserId());//直接加入并集 自动去重
            if (rowUser.getLikelist().contains(colId))
                intersection.add(rowUser.getUserId());//如果该用户还点赞过j位置的帖子，加入交集
        }
        //遍历所有点赞过j 位置帖子的用户
        for (User colUser : colUserList) {
            union.add(colUser.getUserId());//直接加入并集 自动去重
            if (colUser.getLikelist().contains(rowId))
                intersection.add(colUser.getUserId());//如果该用户还点赞过i位置的梯子，加入交集
        }
        int unionSize = union.size();//不可能为空
        int intersectionSize = intersection.size();
        //如果交集为空 直接返回0
        if (intersectionSize == 0) {
            return 0.0;
        }
        return (double) intersectionSize / unionSize;
    }
}

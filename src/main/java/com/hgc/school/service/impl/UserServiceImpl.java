package com.hgc.school.service.impl;

import com.hgc.school.mapper.UserMapper;
import com.hgc.school.service.UserService;
import com.hgc.school.vo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 *
 */
@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserMapper userMapper;

    @Override
    public User selectUser(String username) {
        return userMapper.selectUser(username);
    }

    @Override
    public int add(User user) {
        return userMapper.addUser(user);
    }

    @Override
    public User selectById(Integer userId) {
        return userMapper.selectById(userId);
    }

    @Override
    public boolean hasUserLikedPost(Integer userId, Integer id) {
        User user = userMapper.selectById(userId);
        if(user != null){
            String likeList = user.getLikelist();
            // 检查用户点赞列表中是否包含指定帖子的 ID
            return likeList != null && Arrays.asList(likeList.split(",")).contains(id.toString());
            //这里用户的点赞列表存放是的帖子的id 以 ， 分隔。  Arrays.asList()方法可以把数组转成集合 调用集合的contains()方法可以优雅的判断变量是否在一个字符串中
            //直接for循环也简单粗暴
        }
        return false;
    }

    @Override
    public void addPostToUserLikes(Integer userId, Integer id) {
        User user = userMapper.selectById(userId);
        if(user != null){
            String likelist = user.getLikelist();
            //likelist.isEmpty()代表空字符串
            if(likelist == null || likelist.isEmpty()){
                user.setLikelist(id.toString());
            }else {
                user.setLikelist(likelist + ","+id.toString());
            }
            userMapper.updateLikeList(user);
        }
    }

    @Transactional
    @Override
    public boolean addFollow(Integer userId1, Integer userId2) {
        User user1 = userMapper.selectById(userId1);
        User user2 = userMapper.selectById(userId2);
        if(user1 != null && user2 != null){
            String follows = user1.getFollows();
            String fans = user2.getFans();
            if(follows == null || follows.isEmpty()){
                user1.setFollows(userId2.toString());
            }else if(!Arrays.asList(follows.split(",")).contains(userId2.toString())){
                user1.setFollows(follows+","+ userId2);
            }
            if(fans == null || fans.isEmpty()){
                user2.setFans(userId1.toString());
            }else if(!Arrays.asList(fans.split(",")).contains(userId1.toString())){
                user2.setFans(fans+","+ userId1);
            }
            userMapper.updateFollows(user1);
            userMapper.updateFans(user2);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public List<User> selectFollows(Integer userId) {
        User user = userMapper.selectById(userId);
        String userFollows = user.getFollows();
        ArrayList<User> userList = new ArrayList<>();
        if(userFollows != null) {
            String[] follows = userFollows.split(",");
            if (follows.length > 0 && !Objects.equals(follows[0], "")) {
                for (String follow : follows) {
                    User user1 = userMapper.selectById(Integer.parseInt(follow));
                    userList.add(user1);
                }
            }
        }
        return userList;
    }

    /**
     * 取消关注
     * @param userId1 操作人
     * @param userId2 取消谁
     * @return
     */
    @Transactional
    @Override
    public boolean unFollow(Integer userId1, Integer userId2) {
        User user1 = userMapper.selectById(userId1);
        User user2 = userMapper.selectById(userId2);
        if(user1 != null && user2 != null){
            String follows = user1.getFollows();
            String fans = user2.getFans();
            StringBuffer sb = new StringBuffer();
            if(follows != null && !follows.isEmpty()){
                String[] array = follows.split(",");
                for (String s : array) {
                    int i = Integer.parseInt(s);
                    if(i != userId2){
                        sb.append(i);
                        sb.append(",");
                    }
                }
                if(sb.length() > 0) {
                    sb = sb.deleteCharAt(sb.length() - 1);
                }
                user1.setFollows(sb.toString());
                sb.setLength(0);
            }
            if(fans != null && !fans.isEmpty()){
                String[] array = fans.split(",");
                for (String s : array) {
                    int i = Integer.parseInt(s);
                    if(i != userId1){
                        sb.append(i);
                        sb.append(",");
                    }
                }
                if(sb.length() > 0) {
                    sb = sb.deleteCharAt(sb.length() - 1);
                }
                user2.setFans(sb.toString());
            }
            userMapper.updateFollows(user1);
            userMapper.updateFans(user2);
            return true;
        }else{
            return false;
        }
    }

    @Override
    public String getFollows(Integer userId) {
       return userMapper.selectById(userId).getFollows();
    }

    @Override
    public List<User> selectFans(Integer userId) {
        User user = userMapper.selectById(userId);
        String userFans = user.getFans();
        ArrayList<User> userList = new ArrayList<>();
        if(userFans != null) {
            String[] fans = userFans.split(",");
            if (fans.length > 0 && !Objects.equals(fans[0], "")) {
                for (String fan : fans) {
                    User user1 = userMapper.selectById(Integer.parseInt(fan));
                    userList.add(user1);
                }
            }
        }
        return userList;
    }

    @Override
    public String getFans(Integer userId) {
        return userMapper.selectById(userId).getFans();
    }
}

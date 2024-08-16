package com.hgc.campusechouserservice.mapper;

import com.hgc.campusechomodel.entity.Follower;
import com.hgc.campusechomodel.entity.Following;
import com.hgc.campusechomodel.entity.User;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 *
 */
@Repository
public interface UserMapper {
    User selectUser(String username);

    int addUser(User user);

    User selectById(Integer userId);

    int updateLikeList(User user);

    void updateFollows(User user1);

    void updateFans(User user2);

    List<Integer> selectPart();

    List<Integer> selectIds();

    Following selectFollowing(Integer userId1, Integer userId2);

    void addFollow(Integer userId1, Integer userId2);

    void updateFollow(Integer userId1, Integer userId2,int type);

    List<Following> selectFollowingIds(Integer userId);

    List<Follower> selectFollowerIds(Integer userId,long offset,long limit);

    void addFollower(Follower follower);

    void updateFollower(Follower follower);

    Follower selectFollower(Integer userId, Integer userId1);

    void updateLoginTime(Integer userId);

    Integer selectFollowsCount(Integer userId);

    Following selectIfExists(Integer userId1, Integer userId2);
}

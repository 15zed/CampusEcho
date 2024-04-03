package com.hgc.school.mapper;

import com.hgc.school.vo.User;
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

    List<User> selectPart();

    List<Integer> selectIds();
}

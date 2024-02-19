package com.hgc.school.service;

import com.hgc.school.vo.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 *
 */
public interface UserService {

    User selectUser(String username);

    int add(User user);

    User selectById(Integer userId);

    boolean hasUserLikedPost(Integer userId, Integer id);

    void addPostToUserLikes(Integer userId, Integer id);

    boolean addFollow(Integer userId, Integer userId1);

    List<User> selectFollows(Integer userId);

    boolean unFollow(Integer userId1, Integer userId2);

    String getFollows(Integer userId);

    List<User> selectFans(Integer userId);

    String getFans(Integer userId);
}

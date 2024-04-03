package com.hgc.school.service;


import com.hgc.school.vo.Info;
import com.hgc.school.vo.User;

import java.util.concurrent.Future;

/**
 *
 */
public interface RedisService {

    <T> Future<Integer> add(T obj);

     Future<Integer> delete(Integer id);
}

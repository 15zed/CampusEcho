package com.hgc.campusechopostsservice.service;


import java.util.concurrent.Future;

/**
 *
 */
public interface RedisService {



     Future<Integer> delete(Integer id);

     boolean setUniqueId(String uniqueId);


     String getInfoById(Integer infoId);

     void setInfoById(Integer infoId, String jsonString);
}

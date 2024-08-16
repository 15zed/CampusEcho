package com.hgc.campusechocommon.config;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * es客户端
 */
@Configuration
public class ESConfig {
//    @Bean
//    public RestHighLevelClient restHighLevelClient() {
//        RestHighLevelClient client = new RestHighLevelClient(
//                RestClient.builder(new HttpHost("62.234.165.51", 9200, "http"))
//        );
//        return client;
//    }

    @Bean
    public RestHighLevelClient restHighLevelClient() {
        RestHighLevelClient client = new RestHighLevelClient(
                RestClient.builder(new HttpHost("localhost", 9200, "http"))
        );
        return client;
    }

}

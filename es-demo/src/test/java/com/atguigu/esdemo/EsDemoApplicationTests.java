package com.atguigu.esdemo;

import com.atguigu.esdemo.pojo.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;

@SpringBootTest
class EsDemoApplicationTests {


    @Autowired
    private ElasticsearchRestTemplate restTemplate;

    @Test
    void contextLoads() {

        this.restTemplate.createIndex(User.class);//创建索引库
        this.restTemplate.putMapping(User.class);//什么映射
    }

}

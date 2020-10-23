package com.atguigu.gmall.index.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.config.GmallCahe;
import com.atguigu.gmall.index.config.RedssonConfig;
import com.atguigu.gmall.index.feign.GmallPmsClient;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.apache.commons.lang3.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service("indexService")
public class IndexServiceImpl implements IndexService {


    @Autowired
    private GmallPmsClient pmsClient;


    @Autowired
    private StringRedisTemplate redisTemplate;


    @Autowired
    private RedssonConfig redssonConfig;


    @Autowired
    private RedissonClient redissonClient;

    private static final String KEY_PREFIX = "index:cates:";

    @Override
    public List<CategoryEntity> queryLvlCategory() {
        Resp<List<CategoryEntity>> listResp = this.pmsClient.queryCategoriesByLevelOrPid(1,null);
        if(listResp != null){
            return listResp.getData();
        }
        return new ArrayList<>();
    }

    @Override
    @GmallCahe(prefix ="index:cates:",timeout = 10,random = 1000,lock = "lock")
    public List<CategoryVo> queryLv2WithSubByPid(Long pid) {
//        //1：查询缓存
//        String jsons = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        if(!StringUtils.isEmpty(jsons)){
//            return JSON.parseArray(jsons,CategoryVo.class);
//        }
//
//        //分布式锁
//        RLock lock = this.redissonClient.getLock("lock"+pid);
//        lock.lock();
//        String jsonss = this.redisTemplate.opsForValue().get(KEY_PREFIX + pid);
//        if(!StringUtils.isEmpty(jsonss)){
//            lock.unlock();
//            return JSON.parseArray(jsonss,CategoryVo.class);
//        }

        //2：缓存没有查询数据库
        Resp<List<CategoryVo>> listResp = this.pmsClient.queryCategoryWithSubByPid(pid);
        List<CategoryVo> data = listResp.getData();
//        if(!CollectionUtils.isEmpty(data)){
//            this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(data),10+(int)Math.random()*10, TimeUnit.DAYS);
////            lock.unlock();
//            return data;
//        }else {
//            this.redisTemplate.opsForValue().set(KEY_PREFIX+pid,JSON.toJSONString(data),5+(int)Math.random()*10, TimeUnit.SECONDS);
//        }
//
//        lock.unlock();
        return data;
    }
}

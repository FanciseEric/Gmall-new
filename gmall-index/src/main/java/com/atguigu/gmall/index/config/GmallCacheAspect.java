package com.atguigu.gmall.index.config;

import com.alibaba.fastjson.JSON;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Aspect
@Component
public class GmallCacheAspect {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedissonClient redissonClient;

    @Around("@annotation(com.atguigu.gmall.index.config.GmallCahe)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable{

        //获取切点的方法签名
        MethodSignature signature = (MethodSignature)joinPoint.getSignature();
        //获取方法对象
        Method method = signature.getMethod();

        //获取方法指定的注解对象
        GmallCahe annotation = method.getAnnotation(GmallCahe.class);

        //获取注解的参数
        String prefix = annotation.prefix();
        String param = Arrays.asList(joinPoint.getArgs()).toString();

        //获取方法返回类型
        Class<?> returnType = method.getReturnType();
        //拦截器那代码块  判断缓存中有没有
        String json = this.redisTemplate.opsForValue().get(prefix + param);
        if(StringUtils.isNotBlank(json)){
            return JSON.parseObject(json,returnType);
        }

        //缓存没有的话，加分布式锁
        RLock lock = this.redissonClient.getLock(annotation.lock() + param);
        lock.lock();

        String json2 = this.redisTemplate.opsForValue().get(prefix + param);
        if(StringUtils.isNotBlank(json2)){
            lock.unlock();
            return JSON.parseObject(json2,returnType);
        }


        //指定目标
        Object result = joinPoint.proceed(joinPoint.getArgs());




        //拦截后代码
        this.redisTemplate.opsForValue().set(
                prefix+param,
                JSON.toJSONString(result),
                annotation.timeout()+new Random().nextInt(annotation.random()),
                TimeUnit.MINUTES);
        lock.unlock();
        return result;
    }



}

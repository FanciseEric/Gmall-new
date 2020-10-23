package com.atguigu.gmall.index.config;

import java.lang.annotation.*;

@Target(ElementType.METHOD)//作用在方法上
@Retention(RetentionPolicy.RUNTIME)//运行时注解
@Documented//生成到文档
public @interface GmallCahe {
    /**
     * 缓存前缀
     * @return
     */
    String prefix() default "";


    /**
     * 缓存有效时间
     * @return
     */
    int timeout() default 5;

    /**
     * 防止雪崩设置随机范围
     * @return
     */
    int random() default 5;

    /**
     * 防止击穿设置的分布式锁key
     * @return
     */
    String lock() default "lock";
}

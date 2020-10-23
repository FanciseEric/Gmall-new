package com.atguigu.gmall.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsFilterConfig {



    @Bean
    public CorsWebFilter corsWebFilter(){

        System.out.println("请求进入过滤器");
        //跨域请求配置对象
        CorsConfiguration corsConfiguration = new CorsConfiguration();
        corsConfiguration.addAllowedOrigin("http://localhost:1000");//*表示所有 但是不能携带cookie
        corsConfiguration.addAllowedOrigin("http://127.0.0.1:1000");//*表示所有 但是不能携带cookie
        corsConfiguration.addAllowedMethod("*");//允许所有方法
        corsConfiguration.setAllowCredentials(true);//允许携带cookie
        corsConfiguration.addAllowedHeader("*");//允许所有头信息跨域访问

        //拦截所有请求
        UrlBasedCorsConfigurationSource configurationSource
                = new UrlBasedCorsConfigurationSource();
        configurationSource.registerCorsConfiguration("/**",corsConfiguration);
        return new CorsWebFilter(configurationSource);
    }
}

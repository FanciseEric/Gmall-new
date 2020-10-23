package com.atguigu.gmall.gateway.filter;

import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthGetwayFilter implements GatewayFilter {



    @Autowired
    private JwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        //获取对象
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();


        //从cookie中获取jwt类型的token
        MultiValueMap<String, HttpCookie> cookies = request.getCookies();
        //判断cookie是否为null
        if(CollectionUtils.isEmpty(cookies) || !cookies.containsKey(this.jwtProperties.getCookieName())){
            //拦截这个请求 设置状态码为身份认证未通过
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        HttpCookie cookie = cookies.getFirst(jwtProperties.getCookieName());
        //判断是否为空
        if(cookie==null || StringUtils.isEmpty(cookie.getValue())){
            //拦截这个请求 设置状态码为身份认证未通过
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
        try {
            //解析jwt
            JwtUtils.getInfoFromToken(cookie.getValue(),this.jwtProperties.getPublicKey());
            //放行请求
            return chain.filter(exchange);
        } catch (Exception e) {
            //出现异常 拦截这个请求 设置状态码为身份认证未通过
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }
    }
}

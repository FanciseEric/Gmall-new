package com.atguigu.gmallauth.controller;


import com.atguigu.core.bean.Resp;
import com.atguigu.core.utils.CookieUtils;
import com.atguigu.gmallauth.config.JwtProperties;
import com.atguigu.gmallauth.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@RestController
@RequestMapping("auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private JwtProperties jwtProperties;

    @PostMapping("accredit")
    public Resp<Object> accredit(@RequestParam("username")String userName,
                                 @RequestParam("password")String password,
                                 HttpServletRequest request,
                                 HttpServletResponse response){
        String token = this.authService.accredit(userName, password);

        // 4.把jwt类型的token放入cookie中
        CookieUtils.setCookie(request, response, this.jwtProperties.getCookieName(), token, this.jwtProperties.getExprieTime() * 60);

        return Resp.ok(token);
    }
}

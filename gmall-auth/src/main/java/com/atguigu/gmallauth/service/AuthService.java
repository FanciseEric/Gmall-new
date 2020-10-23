package com.atguigu.gmallauth.service;

import com.atguigu.core.bean.Resp;
import com.atguigu.core.exception.MemberException;
import com.atguigu.core.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmallauth.config.JwtProperties;
import com.atguigu.gmallauth.feign.GmallUmsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service

public class AuthService {

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private GmallUmsClient umsClient;

    public String accredit(String userName, String password) {
        // 1.调用ums的用户查询接口
        Resp<MemberEntity> memberEntityResp = this.umsClient.queryUserByPad(userName, password);
        MemberEntity memberEntity = memberEntityResp.getData();

        // 2.不存在直接返回
        if (memberEntity == null) {
            throw new MemberException("用户名或者密码输入有误！");
        }

        try {
            // 3.制作jwt
            Map<String, Object> map = new HashMap<>();
            map.put("userId", memberEntity.getId());
            map.put("userName", memberEntity.getUsername());
            return JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExprieTime());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}

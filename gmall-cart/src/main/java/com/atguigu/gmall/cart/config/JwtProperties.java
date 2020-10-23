package com.atguigu.gmall.cart.config;


import com.atguigu.core.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.security.PublicKey;

@ConfigurationProperties("auth.jwt")
@Data
@Slf4j
public class JwtProperties {

    private String publicKeyPath;
    private String cookieName;
    private String userKeyName;
    private Integer expireTime;

    private PublicKey publicKey;

    @PostConstruct
    public void init() {


        try {
            this.publicKey = RsaUtils.getPublicKey(publicKeyPath);


        } catch (Exception e) {
            log.error("读取公钥失败");
            log.error("错误信息：" + e);
        }
    }
}

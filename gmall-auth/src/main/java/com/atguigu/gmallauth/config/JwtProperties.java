package com.atguigu.gmallauth.config;


import com.atguigu.core.utils.RsaUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@ConfigurationProperties("auth.jwt")
@Data
@Slf4j
public class JwtProperties {

    private String privateKeyPath;
    private String publicKeyPath;
    private String secret;
    private String cookieName;
    private Integer exprieTime;


    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void init() {


        try {
            File priFile = new File(privateKeyPath);
            File pubFile = new File(publicKeyPath);
            //判断文件是否存在

            if (!priFile.exists() || !pubFile.exists()) {
                RsaUtils.generateKey(publicKeyPath, privateKeyPath, secret);
            }
            //存在就读取  并赋值给键
            this.privateKey = RsaUtils.getPrivateKey(privateKeyPath);
            this.publicKey = RsaUtils.getPublicKey(publicKeyPath);
        } catch (Exception e) {
            log.error("生成公钥私钥失败");
        }
    }
}

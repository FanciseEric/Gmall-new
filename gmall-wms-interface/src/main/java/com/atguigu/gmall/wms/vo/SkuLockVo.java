package com.atguigu.gmall.wms.vo;


import lombok.Data;

@Data
public class SkuLockVo {

    private Long skuId; //商品id

    private Integer count;//锁定的数量

    private Boolean lock;//锁定状态

    private Long wareSkuId;//锁定的仓库id

    private String orderToken;
}

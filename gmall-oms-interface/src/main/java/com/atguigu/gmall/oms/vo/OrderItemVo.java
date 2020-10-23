package com.atguigu.gmall.oms.vo;


import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderItemVo {

    private Long skuId;
    private String image;
    private String title;//标题
    private List<SkuSaleAttrValueEntity> saleAttrs;//销售属性

    private BigDecimal price;//时时价格
    private BigDecimal weight;//重量
    private BigDecimal count;

    private List<ItemSaleVo> sales;//营销属性

    private Boolean store;
}

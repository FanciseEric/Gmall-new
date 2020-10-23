package com.atguigu.gmall.cart.entity;

import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class Cart {

    private Long skuId;
    private Boolean check;//选中状态
    private String image;
    private String title;//标题
    private List<SkuSaleAttrValueEntity> saleAttrs;//销售属性

    private BigDecimal price;//加入购物车时的价格
    private BigDecimal currentPrice;//时时价格
    private BigDecimal count;

    private List<ItemSaleVo> sales;//营销属性

}

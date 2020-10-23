package com.atguigu.gmall.item.vo;


import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ItemVo {


    //分类
    private Long categoryId;
    private String categoryName;

    //品牌
    private Long brandId;
    private String brandName;

    //spu
    private Long spuId;
    private String spuName;

    //sku
    private Long skuId;
    private String skuTitle;
    private String skuSubTitle;
    private BigDecimal price;
    private BigDecimal weight;

    //营销
    private List<ItemSaleVo> sales;

    //是否有货
    private Boolean store = false;

    //销售属性
    private List<SkuSaleAttrValueEntity> saleAttrs;

    //sku图片
    private List<SkuImagesEntity> images;

    //海报信息
    private List<String> desc;

    //分组及组下的规格参数
    private List<ItemGroupVo> groups;
}

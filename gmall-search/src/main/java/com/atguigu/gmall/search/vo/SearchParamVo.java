package com.atguigu.gmall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchParamVo {

    private String keyword;//用户输入的检索关键字

    private Long[] brandId;//品牌多个id

    private Long[] categoryId;//分类id

    private String[] props;//规格参数过滤

    private String order;//排序

    private Boolean store;//是否有货


    //价格区间
    private Double priceFrom;
    private Double priceTo;

    //分页
    private Integer pageNum = 1;
    private final Integer pageSize = 50;

}

package com.atguigu.gmall.search.vo;

import lombok.Data;

import java.util.List;

@Data
public class SearchResponseVo {

    private SearchResponseAttrVo brand;
    private SearchResponseAttrVo category;

    private List<SearchResponseAttrVo> attrs;//过滤条件封装

    private Long total;//总记录数

    private Integer pageNum;//页码

    private Integer pageSize;//每页大小

    private List<GoodsVO> data;
}

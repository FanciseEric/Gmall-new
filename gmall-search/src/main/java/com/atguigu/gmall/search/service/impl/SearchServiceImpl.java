package com.atguigu.gmall.search.service.impl;


import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.search.service.SearchService;
import com.atguigu.gmall.search.vo.GoodsVO;
import com.atguigu.gmall.search.vo.SearchParamVo;
import com.atguigu.gmall.search.vo.SearchResponseAttrVo;
import com.atguigu.gmall.search.vo.SearchResponseVo;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.text.Text;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service("SearchService")
public class SearchServiceImpl implements SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;


    @Override
    public SearchResponseVo search(SearchParamVo searchParamVo) {

        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, builder(searchParamVo));
            SearchResponse response = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            SearchResponseVo responseVo = parseResult(response);
            responseVo.setPageNum(searchParamVo.getPageNum());
            responseVo.setPageSize(searchParamVo.getPageSize());
            return responseVo;
        } catch (IOException e) {
            return new SearchResponseVo();
        }
    }



    /**
     * 构建查询条件
     * @param searchParamVo
     * @return
     */
    private SearchSourceBuilder builder(SearchParamVo searchParamVo){
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        //构建查询条件和过滤条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();//构建bool层
        sourceBuilder.query(boolQueryBuilder);
        //1:构建匹配查询
        String keyword = searchParamVo.getKeyword();
        if(StringUtils.isEmpty(keyword)){
            //返回空数据
            return new SearchSourceBuilder();
        }
        boolQueryBuilder.must(QueryBuilders.matchQuery("title",keyword).operator(Operator.AND));//条件查询



        //2：构建过滤
        Long[] brandId = searchParamVo.getBrandId();
        if(brandId != null && brandId.length>=0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("brandId",brandId));
        }

        //3：构建分类过滤

        Long[] categoryId = searchParamVo.getCategoryId();
        if(categoryId != null && categoryId.length>0){
            boolQueryBuilder.filter(QueryBuilders.termsQuery("categoryId",categoryId));
        }
        //4:价格区间过滤
        Double priceFrom = searchParamVo.getPriceFrom();
        Double priceTo = searchParamVo.getPriceTo();
        if(priceFrom != null || priceTo != null){
            RangeQueryBuilder rangeQuery = QueryBuilders.rangeQuery("price");
            if(priceFrom !=null){
                rangeQuery.gte(priceFrom);
            }
            if(priceTo!=null){
                rangeQuery.lte(priceTo);
            }
            boolQueryBuilder.filter(rangeQuery);
        }

        //5:是否有货
        Boolean store = searchParamVo.getStore();
        if(store !=null){
            boolQueryBuilder.filter(QueryBuilders.termQuery("store",store));
        }
        //6:规格参数的嵌套过滤
        String[] props = searchParamVo.getProps();
        if(props != null && props.length > 0){
            for (String prop : props) {
                String[] attr = StringUtils.split(prop, ":");
                if(attr.length!=2 && attr==null){
                    continue;
                }
                BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
                boolQuery.must(QueryBuilders.termsQuery("attrs.attrId",attr[0]));//规格参数
                String[] value = StringUtils.split(attr[1], "-");
                boolQuery.must(QueryBuilders.termsQuery("attrs.attrValue",value));//规格参数的id
                boolQueryBuilder.filter(QueryBuilders.nestedQuery("attrs",boolQuery, ScoreMode.None));
            }
        }


        //7：构建排序
        String order = searchParamVo.getOrder();
        if(StringUtils.isNotBlank(order)){
            String[] sorts = StringUtils.split(order, ":");

            if(sorts!=null && sorts.length==2){
                String sortFild = "_score";
                switch (sorts[0]){
                    //价格排序
                    case "1":
                        sortFild=  "price";
                        break;
                    //销量排序
                    case "2":
                        sortFild=  "sales";
                        break;
                        //新品
                    case "3":
                        sortFild=  "coreteTime";
                        break;
                    default:
                        break;
                }
              sourceBuilder.sort(sortFild,StringUtils.equals("desc",sorts[1])? SortOrder.DESC:SortOrder.ASC);
            }
        }


        //8:构建分页
        Integer pageNum = searchParamVo.getPageNum();
        Integer pageSize = searchParamVo.getPageSize();
        sourceBuilder.from((pageNum-1)*pageSize);
        sourceBuilder.size(pageSize);

        //9:构建高亮

        sourceBuilder.highlighter(new HighlightBuilder()
                .field("title")
                .preTags("<font style='color:red'>")
                .postTags("</font>")
        );


        //10:品牌聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("brandIdAgg").field("brandId")
        .subAggregation(AggregationBuilders.terms("brandNameAgg").field("brandName")));

        //11:分类聚合
        sourceBuilder.aggregation(AggregationBuilders.terms("categoryIdAgg").field("categoryId")
        .subAggregation(AggregationBuilders.terms("categoryNameAgg").field("categoryName")));


        //12:规格参数聚合
        sourceBuilder.aggregation(AggregationBuilders.nested("attrsAgg","attrs")
        .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
        .subAggregation(AggregationBuilders.terms("aggrValueAgg").field("attrs.attrValue"))
        )

        );

        //13:结果集过滤
        sourceBuilder.fetchSource(new String[]{"skuId","title","price","pic"},null);
        return sourceBuilder;
    }


    /**
     * 解析结果集
     * @param response
     * @return
     */
    private SearchResponseVo parseResult(SearchResponse response){
        SearchResponseVo responseVo = new SearchResponseVo();
        SearchHits hits = response.getHits();//获取hits对象
        responseVo.setTotal(hits.getTotalHits());//总记录数

        //解析hits 查询记录
        SearchHit[] hitsHits = hits.getHits();
        //遍历对象数组
        List<GoodsVO> goodsVOS = new ArrayList<>();
        for (SearchHit hitsHit : hitsHits) {
            String goodsVoJson = hitsHit.getSourceAsString();
            GoodsVO goodsVO = JSON.parseObject(goodsVoJson, GoodsVO.class);
            //获取高亮结果集
            HighlightField highlightField = hitsHit.getHighlightFields().get("title");
            Text texts = highlightField.getFragments()[0];
            goodsVO.setTitle(texts.string());
            goodsVOS.add(goodsVO);
        }
        responseVo.setData(goodsVOS);

        //获取品牌
        Map<String, Aggregation> aggsMap = response.getAggregations().asMap();
        //获取品牌聚合
        ParsedLongTerms brandIdAgg = (ParsedLongTerms)aggsMap.get("brandIdAgg");
        //获取桶
        List<? extends Terms.Bucket> buckets = brandIdAgg.getBuckets();
        //每一个桶转化成  id：1,name:name
        if(!CollectionUtils.isEmpty(buckets)){
            List<String> brandValues = buckets.stream().map(bucket -> {
                HashMap<String, Object> map = new HashMap<>();
                Long brandId = bucket.getKeyAsNumber().longValue();
                ParsedStringTerms brandNameAgg = (ParsedStringTerms) bucket.getAggregations().get("brandNameAgg");
                String brandName = brandNameAgg.getBuckets().get(0).getKeyAsString();
                map.put("id", brandId);
                map.put("name", brandName);
                String brandValue = JSON.toJSONString(map);
                return brandValue;
            }).collect(Collectors.toList());

            SearchResponseAttrVo brandVo = new SearchResponseAttrVo();
            brandVo.setAttrName("品牌");
            brandVo.setAttrValues(brandValues);
            responseVo.setBrand(brandVo);
        }




        //获取分类
        ParsedLongTerms categoryIdAgg = (ParsedLongTerms)aggsMap.get("categoryIdAgg");
        List<? extends Terms.Bucket> categoryBuckets = categoryIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(categoryBuckets)){
            List<String> categoryValue = categoryBuckets.stream().map(categoryBucket -> {
                HashMap<String, Object> map = new HashMap<>();
                Long categoryId = ((Terms.Bucket) categoryBucket).getKeyAsNumber().longValue();
                map.put("id",categoryId);
                ParsedStringTerms categoryNameAgg = ((Terms.Bucket) categoryBucket).getAggregations().get("categoryNameAgg");
                map.put("name",categoryNameAgg.getBuckets().get(0).getKeyAsString());

                return JSON.toJSONString(map);
            }).collect(Collectors.toList());
            SearchResponseAttrVo attrVo = new SearchResponseAttrVo();
            attrVo.setAttrName("分类");
            attrVo.setAttrValues(categoryValue);
            responseVo.setCategory(attrVo);
        }


        //获取规格参数
        ParsedNested attrsAgg = (ParsedNested)aggsMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<? extends Terms.Bucket> attrBuckets = attrIdAgg.getBuckets();
        if(!CollectionUtils.isEmpty(attrBuckets)){
            List<SearchResponseAttrVo> attrVoList = attrBuckets.stream().map(attrBucket -> {
                SearchResponseAttrVo attrVo = new SearchResponseAttrVo();


                attrVo.setAttrId(((Terms.Bucket) attrBucket).getKeyAsNumber().longValue());
                ParsedStringTerms attrNameAgg = ((Terms.Bucket) attrBucket).getAggregations().get("attrNameAgg");
                attrVo.setAttrName(attrNameAgg.getBuckets().get(0).getKeyAsString());
                ParsedStringTerms attrValueAgg =((Terms.Bucket) attrBucket).getAggregations().get("aggrValueAgg");
                List<? extends Terms.Bucket> valueAggBuckets = attrValueAgg.getBuckets();
                if(!CollectionUtils.isEmpty(valueAggBuckets)){
                    List<String> attrValues = valueAggBuckets.stream().map(Terms.Bucket::getKeyAsString).collect(Collectors.toList());
                    attrVo.setAttrValues(attrValues);
                }
                return attrVo;
            }).collect(Collectors.toList());


            responseVo.setAttrs(attrVoList);
        }
        return responseVo;
    }
}

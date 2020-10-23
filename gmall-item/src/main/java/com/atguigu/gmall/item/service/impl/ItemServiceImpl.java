package com.atguigu.gmall.item.service.impl;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.item.config.ThreadPoolConfig;
import com.atguigu.gmall.item.feign.GmallPmsClient;
import com.atguigu.gmall.item.feign.GmallSmsClient;
import com.atguigu.gmall.item.feign.GmallWmsClient;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.atguigu.gmall.item.vo.ItemVo;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.AttrGroupVo;
import com.atguigu.gmall.sms.api.GmallSmsApi;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.wms.api.GmallWmsApi;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

@Service("ItemService")
public class ItemServiceImpl implements ItemService {
    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsApi;

    @Autowired
    private GmallPmsClient pmsApi;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;


    @Override
    public ItemVo queryItemBySkuId(Long skuId) {

        ItemVo itemVo = new ItemVo();

        // sku相关信息
        CompletableFuture<SkuInfoEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
            Resp<SkuInfoEntity> skuInfoEntityResp = this.pmsApi.querySkuById(skuId);
            SkuInfoEntity skuInfoEntity = skuInfoEntityResp.getData();
            if (skuInfoEntity == null) {
                return null;
            }
            itemVo.setSkuId(skuId);
            itemVo.setSkuTitle(skuInfoEntity.getSkuTitle());
            itemVo.setSkuSubTitle(skuInfoEntity.getSkuSubtitle());
            itemVo.setPrice(skuInfoEntity.getPrice());
            itemVo.setWeight(skuInfoEntity.getWeight());
            return skuInfoEntity;
        }, threadPoolExecutor);

        //营销信息
        CompletableFuture<Void> saleFuture = CompletableFuture.runAsync(() -> {
            List<ItemSaleVo> itemSaleVos
                    = this.smsClient.queryItemSaleBySkuId(skuId).getData();

            itemVo.setSales(itemSaleVos);
        }, threadPoolExecutor);


        //库存信息
        CompletableFuture<Void> wareFuture = CompletableFuture.runAsync(() -> {
            List<WareSkuEntity> wareSkuEntities = this.wmsApi.queryWareSkuBySkuId(skuId).getData();
            if (!CollectionUtils.isEmpty(wareSkuEntities)) {
                itemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
            }
        }, threadPoolExecutor);


        //sku图片
        CompletableFuture<Void> imageFuture = CompletableFuture.runAsync(() -> {
            List<SkuImagesEntity> skuImagesEntities = this.pmsApi.querySkuyImagesBySkuId(skuId).getData();
            itemVo.setImages(skuImagesEntities);
        }, threadPoolExecutor);

        //品牌
        CompletableFuture<Void> brandFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
            BrandEntity brandEntity = this.pmsApi.queryBrandById(skuInfoEntity.getBrandId()).getData();
            if (brandEntity != null) {
                itemVo.setBrandId(brandEntity.getBrandId());
                itemVo.setBrandName(brandEntity.getName());
            }
        }, threadPoolExecutor);


        //分类
        CompletableFuture<Void> categoryFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
            CategoryEntity categoryEntity = this.pmsApi.queryCategoryById(skuInfoEntity.getCatalogId()).getData();
            if (categoryEntity != null) {
                itemVo.setCategoryId(categoryEntity.getCatId());
                itemVo.setCategoryName(categoryEntity.getName());
            }
        }, threadPoolExecutor);


        //spu信息
        CompletableFuture<Void> spuFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
            Long spuId = skuInfoEntity.getSpuId();
            SpuInfoEntity spuInfoEntity = this.pmsApi.querySpuById(spuId).getData();
            if (spuInfoEntity != null) {
                itemVo.setSpuId(spuInfoEntity.getId());
                itemVo.setSpuName(spuInfoEntity.getSpuName());
            }
        }, threadPoolExecutor);


        //销售属性
        CompletableFuture<Void> skuSaleFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
            Long spuId = skuInfoEntity.getSpuId();
            List<SkuSaleAttrValueEntity> attrValueEntityList = this.pmsApi.querySaleAttrBySpuId(spuId).getData();
            itemVo.setSaleAttrs(attrValueEntityList);
        }, threadPoolExecutor);


        //组及组下的规格参数
        CompletableFuture<Void> groupFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
            List<ItemGroupVo> itemGroupVos = this.pmsApi.queryItemGroupsBySpuIdAndCid(skuInfoEntity.getSpuId(), skuInfoEntity.getCatalogId()).getData();
            itemVo.setGroups(itemGroupVos);
        }, threadPoolExecutor);


        //描述
        CompletableFuture<Void> descFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
            SpuInfoDescEntity spuInfoDescEntity = this.pmsApi.querySpuDescBySpuId(skuInfoEntity.getSpuId()).getData();
            if (spuInfoDescEntity != null) {
                String[] descs = StringUtils.split(spuInfoDescEntity.getDecript(), ",");
                itemVo.setDesc(Arrays.asList(descs));
            }
        }, threadPoolExecutor);



        //阻塞线程，等待全部完成进行返回
        CompletableFuture.allOf(saleFuture,wareFuture,imageFuture,brandFuture,categoryFuture,spuFuture,skuSaleFuture,groupFuture,descFuture).join();
        return itemVo;
    }
}


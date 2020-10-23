package com.atguigu.gmall.pms.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.pms.dao.*;
import com.atguigu.gmall.pms.entity.SkuImagesEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.pms.entity.SpuInfoEntity;
import com.atguigu.gmall.pms.feign.GmallSmsClient;
import com.atguigu.gmall.pms.service.SpuInfoService;
import com.atguigu.gmall.pms.vo.BaseAttrValueVo;
import com.atguigu.gmall.pms.vo.SkuInfoVo;
import com.atguigu.gmall.pms.vo.SpuInfoVo;
import com.atguigu.gmall.sms.vo.SkuSaleVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import io.seata.spring.annotation.GlobalTransactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Date;
import java.util.List;
import java.util.UUID;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {
    @Autowired
    private SpuInfoDescDao descDao;

    @Autowired
    private SpuInfoDescServiceImpl descService;

    @Autowired
    private ProductAttrValueDao baseAttrDao;

     @Autowired
     private SkuInfoDao skuInfoDao;

     @Autowired
     private SkuImagesDao skuImagesDao;

     @Autowired
     private SkuSaleAttrValueDao saleAttrDao;

     @Autowired
     private GmallSmsClient smsClient;

     @Autowired
     private AmqpTemplate amqpTemplate;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo querySpuByCidPage(QueryCondition queryCondition, Long cid) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        //:关键字
        String key = queryCondition.getKey();
        if(StringUtils.isNotBlank(key)){
//            wrapper.eq("id",key).or().like("spu_name",key);
            wrapper.and(t -> t.eq("id",key).or().like("spu_name",key));
        }
        //：分类id
        if(cid!=0){
            wrapper.eq("catalog_id",cid);
        }

        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(queryCondition), wrapper);
        return new PageVo(page);
    }

    @GlobalTransactional(timeoutMills = 10000)
    @Override
    public void bigSave(SpuInfoVo spuInfoVo) {

        //1:保存spu信息
            //1.1：保存spuInfo信息
        spuInfoVo.setCreateTime(new Date());
        spuInfoVo.setUodateTime(spuInfoVo.getCreateTime());
        this.save(spuInfoVo);
        Long spuId = spuInfoVo.getId();
        //1.2：保存spuInfoDesc信息
        this.descService.saveSpuInfoDesc(spuInfoVo,spuId);

        //1.3:保存基本属性
        List<BaseAttrValueVo> baseAttrs = spuInfoVo.getBaseAttrs();
        if(!CollectionUtils.isEmpty(baseAttrs)){
            baseAttrs.forEach(baseAttrValueVo ->{
                baseAttrValueVo.setSpuId(spuId);
                this.baseAttrDao.insert(baseAttrValueVo);
            });
        }

        //2:保存sku信息

        List<SkuInfoVo> skus = spuInfoVo.getSkus();
        if(CollectionUtils.isEmpty(skus)){
            return;
        }
        skus.forEach(skuInfoVo  ->{
            //2.1:保存skuINfo
            skuInfoVo.setSpuId(spuId);
            skuInfoVo.setSkuCode(UUID.randomUUID().toString());
            skuInfoVo.setCatalogId(spuInfoVo.getCatalogId());
            skuInfoVo.setBrandId(spuInfoVo.getBrandId());
            //默认图片设置
            List<String> images = skuInfoVo.getImages();
            if(!CollectionUtils.isEmpty(images)){
                skuInfoVo.setSkuDefaultImg(StringUtils.isNotBlank(skuInfoVo.getSkuDefaultImg()) ? skuInfoVo.getSkuDefaultImg():images.get(0));
            }
            this.skuInfoDao.insert(skuInfoVo);
            Long skuId = skuInfoVo.getSkuId();
            //2.2:保存sku图形信息
            if(!CollectionUtils.isEmpty(images)){
                images.forEach(image ->{
                    SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                    skuImagesEntity.setImgUrl(image);
                    skuImagesEntity.setSkuId(skuId );
                    if(StringUtils.equals(image,skuInfoVo.getSkuDefaultImg())){
                        skuImagesEntity.setDefaultImg(1);
                    }else {
                        skuImagesEntity.setDefaultImg(0);
                    }
                    skuImagesDao.insert(skuImagesEntity);
                });
            }
            //2.3:保存销售属性
            List<SkuSaleAttrValueEntity> saleAttrs = skuInfoVo.getSaleAttrs();
            if(!CollectionUtils.isEmpty(saleAttrs)){

                saleAttrs.forEach(skuSaleAttrValueEntity ->{
                    skuSaleAttrValueEntity.setSkuId(skuId);
                    this.saleAttrDao.insert(skuSaleAttrValueEntity);
                });
            }


            //3：保存营销信息
            SkuSaleVo skuSaleVo = new SkuSaleVo();
            BeanUtils.copyProperties(skuInfoVo,skuSaleVo);
            this.smsClient.saveSkuSales(skuSaleVo);


        });

        /**
         * 发送消息到队列
         * 交换机：pms-spu-exchange
         * routingkey:item.insert
         */
        this.amqpTemplate.convertAndSend("pms-spu-exchange","item.insert",spuId);
    }

}
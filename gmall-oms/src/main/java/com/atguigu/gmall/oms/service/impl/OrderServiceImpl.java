package com.atguigu.gmall.oms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.oms.dao.OrderItemDao;
import com.atguigu.gmall.oms.entity.OrderItemEntity;
import com.atguigu.gmall.oms.feign.GmallPmsClient;
import com.atguigu.gmall.oms.feign.GmallUmsClient;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.oms.dao.OrderDao;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.service.OrderService;
import org.springframework.transaction.annotation.Transactional;


@Service("orderService")
public class OrderServiceImpl extends ServiceImpl<OrderDao, OrderEntity> implements OrderService {

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private OrderItemDao itemDao;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<OrderEntity> page = this.page(
                new Query<OrderEntity>().getPage(params),
                new QueryWrapper<OrderEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public OrderEntity saveOrder(OrderSubmitVo submitVo) {
        //保存订单
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setMemberId(submitVo.getUserId());
        orderEntity.setOrderSn(submitVo.getOrderToken());
        orderEntity.setCreateTime(new Date());

        MemberEntity memberEntity = this.umsClient.queryMemberById(submitVo.getUserId()).getData();
        orderEntity.setMemberUsername(memberEntity.getUsername());
        orderEntity.setTotalAmount(submitVo.getTotalPrice());
        orderEntity.setPayType(submitVo.getPayType());
        orderEntity.setSourceType(0);
        orderEntity.setStatus(0);
        orderEntity.setDeliveryCompany(submitVo.getDeliveryCompany());
        // TODO 成长积分  购物积分等
        MemberReceiveAddressEntity address = submitVo.getAddress();
        orderEntity.setReceiverRegion(address.getRegion());
        orderEntity.setReceiverPostCode(address.getPostCode());
        orderEntity.setReceiverProvince(address.getProvince());
        orderEntity.setReceiverPhone(address.getPhone());
        orderEntity.setReceiverName(address.getName());
        orderEntity.setReceiverDetailAddress(address.getDetailAddress());
        orderEntity.setReceiverCity(address.getCity());
        orderEntity.setDeleteStatus(0);
        orderEntity.setUseIntegration(submitVo.getBounds());
        this.save(orderEntity);

        //保存订单详情
        List<OrderItemVo> items =
                submitVo.getItems();

        if(!CollectionUtils.isEmpty(items)){
            items.forEach(item ->{
                OrderItemEntity orderItemEntity = new OrderItemEntity();
                orderItemEntity.setOrderId(orderEntity.getId());
                orderItemEntity.setOrderSn(submitVo.getOrderToken());
                //sku
                SkuInfoEntity skuInfoEntity = this.pmsClient.querySkuById(item.getSkuId()).getData();
                orderItemEntity.setSkuId(item.getSkuId());
                orderItemEntity.setSkuPrice(skuInfoEntity.getPrice());
                orderItemEntity.setSkuPic(skuInfoEntity.getSkuDefaultImg());
                orderItemEntity.setSkuQuantity(item.getCount().intValue());
                orderItemEntity.setSkuName(skuInfoEntity.getSkuName());
                //销售属性
                List<SkuSaleAttrValueEntity> saleAttrValues
                        = this.pmsClient.querySaleAttrBySkuId(item.getSkuId()).getData();
                orderItemEntity.setSkuAttrsVals(JSON.toJSONString(saleAttrValues));
                //spu
                SpuInfoEntity spuInfoEntity = this.pmsClient.querySpuById(skuInfoEntity.getSpuId()).getData();
                //描述
                SpuInfoDescEntity spuInfoDesc
                        = this.pmsClient.querySpuDescBySpuId(spuInfoEntity.getId()).getData();
                orderItemEntity.setSpuPic(spuInfoDesc.getDecript());
                orderItemEntity.setSpuId(spuInfoEntity.getId());
                orderItemEntity.setSpuName(spuInfoEntity.getSpuName());
                //品牌名称
                BrandEntity brandEntity = this.pmsClient.queryBrandById(skuInfoEntity.getBrandId()).getData();
                orderItemEntity.setSpuBrand(brandEntity.getName());

                orderItemEntity.setCategoryId(spuInfoEntity.getCatalogId());

                this.itemDao.insert(orderItemEntity);

            });
        }

        //创建成功  开始定时关单
        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","order.ttl",submitVo.getOrderToken());
        return orderEntity;
    }

}
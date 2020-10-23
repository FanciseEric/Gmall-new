package com.atguigu.gmall.order.service;


import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.core.exception.MemberException;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.order.config.ThreadPoolConfig;
import com.atguigu.gmall.order.frign.*;
import com.atguigu.gmall.order.interceptor.LoginInterceptor;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class OrderService {

    @Autowired
    private GmallCartClient cartClient;

    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallUmsClient umsClient;

    @Autowired
    private GmallOmsClient omsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @Autowired
    private ThreadPoolConfig threadPoolConfig;
    private static final String order_key_prefix = "order:token:";


    public OrderConfirmVo confirm() {
        OrderConfirmVo confirmVo = new OrderConfirmVo();
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        if(userId == null){
            throw new MemberException("用户登录过期！");
        }


        //详情
        CompletableFuture<Void> cartFuture = CompletableFuture.supplyAsync(() -> {
            List<Cart> cartList =
                    this.cartClient.queryCheckCarts(userId).getData();
            return cartList;
        },threadPoolConfig.threadPoolExecutor()).thenAcceptAsync(cartList -> {
            List<OrderItemVo> items = cartList.stream().map(cart -> {
                //保证数据实时同步，重新查询数据库
                OrderItemVo orderItemVo = new OrderItemVo();

                CompletableFuture<SkuInfoEntity> skuFuture = CompletableFuture.supplyAsync(() -> {
                    SkuInfoEntity skuInfoEntity = this.pmsClient.querySkuById(cart.getSkuId()).getData();
                    if (skuInfoEntity != null) {
                        orderItemVo.setSkuId(cart.getSkuId());
                        orderItemVo.setCount(cart.getCount());
                        orderItemVo.setTitle(skuInfoEntity.getSkuTitle());
                        orderItemVo.setPrice(skuInfoEntity.getPrice());
                        orderItemVo.setImage(skuInfoEntity.getSkuDefaultImg());
                        orderItemVo.setWeight(skuInfoEntity.getWeight());
                    }
                    return skuInfoEntity;
                },threadPoolConfig.threadPoolExecutor());

                //查询销售属性
                CompletableFuture<Void> saleAttrFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
                    if (skuInfoEntity != null) {

                        List<SkuSaleAttrValueEntity> saleAttrs
                                = this.pmsClient.querySaleAttrBySkuId(cart.getSkuId()).getData();
                        orderItemVo.setSaleAttrs(saleAttrs);
                    }
                },threadPoolConfig.threadPoolExecutor());


                //营销信息
                CompletableFuture<Void> itemSaleFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
                    if (skuInfoEntity != null) {
                        List<ItemSaleVo> itemSales = this.smsClient.queryItemSaleBySkuId(cart.getSkuId()).getData();
                        orderItemVo.setSales(itemSales);
                    }
                },threadPoolConfig.threadPoolExecutor());

                //查询库存信息
                CompletableFuture<Void> wareFuture = skuFuture.thenAcceptAsync(skuInfoEntity -> {
                    if (skuInfoEntity != null) {
                        List<WareSkuEntity> wareSkuEntities = this.wmsClient.queryWareSkuBySkuId(cart.getSkuId()).getData();
                        if (wareSkuEntities != null) {
                            orderItemVo.setStore(wareSkuEntities.stream().anyMatch(wareSkuEntity -> wareSkuEntity.getStock() > 0));
                        }
                    }
                },threadPoolConfig.threadPoolExecutor());

                CompletableFuture.allOf(saleAttrFuture,itemSaleFuture,wareFuture).join();
                return orderItemVo;
            }).collect(Collectors.toList());
            confirmVo.setItems(items);
            System.out.println(Thread.currentThread().getName()+"正在执行详情");
        },threadPoolConfig.threadPoolExecutor());


        //地址列表
        CompletableFuture<Void> addressFuture = CompletableFuture.runAsync(() -> {
            List<MemberReceiveAddressEntity> addressEntities = this.umsClient.queryAddressesByUserId(userId).getData();
            confirmVo.setAddresses(addressEntities);
            System.out.println(Thread.currentThread().getName()+"正在执行地址列表");
        },threadPoolConfig.threadPoolExecutor());


        //可用积分
        CompletableFuture<Void> memberFuture = CompletableFuture.runAsync(() -> {
            MemberEntity memberEntity = this.umsClient.queryMemberById(userId).getData();
            if (memberEntity != null) {
                confirmVo.setBounds(memberEntity.getIntegration());
                System.out.println(Thread.currentThread().getName()+"正在执行可用积分");
            }
        },threadPoolConfig.threadPoolExecutor());



        //防重标志
        CompletableFuture<Void> tokenFuture = CompletableFuture.runAsync(() -> {
            String orderToken = IdWorker.getTimeId();
            this.redisTemplate.opsForValue().set(order_key_prefix + orderToken, orderToken, 3, TimeUnit.HOURS);
            confirmVo.setOrderToken(orderToken);
            System.out.println(Thread.currentThread().getName()+"正在执行防重标志");
        },threadPoolConfig.threadPoolExecutor());


        CompletableFuture.allOf(cartFuture,addressFuture,memberFuture,tokenFuture).join();
        return confirmVo;
    }

    public OrderEntity submit(OrderSubmitVo submitVo) {
        //1:防重
        String orderToken = submitVo.getOrderToken();
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] " +
                "then return redis.call('del', KEYS[1]) " +
                "else return 0 end";
        Boolean flag =  this.redisTemplate.execute(new DefaultRedisScript<>(script,Boolean.class), Arrays.asList(order_key_prefix+orderToken),orderToken);
        if(!flag){
            throw new MemberException("多次提交过快，请稍后再试！");
        }
        //2：验证总价
        BigDecimal totalPrice = submitVo.getTotalPrice();
        List<OrderItemVo> items = submitVo.getItems();//订单详情
        if(CollectionUtils.isEmpty(items)){
            throw new MemberException("您没有选中的商品！");
        }
        //遍历订单详情  获取数据库价格  计算实时价格
        BigDecimal currentTotalPrice = items.stream().map(item -> {
            SkuInfoEntity skuInfoEntity = this.pmsClient.querySkuById(item.getSkuId()).getData();
            if (skuInfoEntity != null) {
                return skuInfoEntity.getPrice().multiply(item.getCount());
            }
            return new BigDecimal(0);
        }).reduce((t1, t2) -> t1.add(t2)).get();

        if(totalPrice.compareTo(currentTotalPrice) != 0){
            throw new MemberException("页面已过期，刷新后在试！");
        }
        //3：验证库存  锁定库存
        List<SkuLockVo> lockVos = items.stream().map(item -> {
            SkuLockVo skuLockVo = new SkuLockVo();
            skuLockVo.setSkuId(item.getSkuId());
            skuLockVo.setCount(item.getCount().intValue());
            skuLockVo.setOrderToken(submitVo.getOrderToken());
            return skuLockVo;
        }).collect(Collectors.toList());
        List<SkuLockVo> skuLockVos = this.wmsClient.checkAndLock(lockVos).getData();

        if(!CollectionUtils.isEmpty(skuLockVos)){
            throw new MemberException("商品库存不足:" + JSON.toJSONString(skuLockVos));
        }
        //4:下单
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        Long userId = userInfo.getUserId();
        submitVo.setUserId(userId);
        OrderEntity orderEntity  = null;
        try {
            orderEntity = this.omsClient.saveOrder(submitVo).getData();
        } catch (Exception e) {
            //订单创建失败  立马释放库存
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","wms.unlock",orderToken);
            //订单创建失败  标记为无效订单
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","oms.close",orderToken);
            e.printStackTrace();
        }
        //5:删除购物车
        HashMap<String, Object> map = new HashMap<>();
        map.put("userId",userId);
        List<Long> skuIds = items.stream().map(OrderItemVo::getSkuId).collect(Collectors.toList());
        map.put("skuIds",JSON.toJSONString(skuIds));
        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","cart.delete",map);
        return orderEntity;
    }
}

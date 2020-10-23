package com.atguigu.gmall.wms.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class StockListener {

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String key_prefix =  "wms:lock:";


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "wms-unlock-queue",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"wms.unlock"}
    ))
    public void unlockListener(String orderToken){
        log.error("进入解锁库存MQ监听-----------------------");
        //获取redis的信息
        String lockjson = this.redisTemplate.opsForValue().get(key_prefix + orderToken);
        if(StringUtils.isEmpty(lockjson)){
            return;
        }
        List<SkuLockVo> lockVos = JSON.parseArray(lockjson, SkuLockVo.class);
        lockVos.forEach(skuLockVo -> {
            this.wareSkuDao.minusStock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
        });
    }


    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "wms-pay-queue",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"stock.minus"}
    ))
    public void minusSStockListener(String orderToken){
        //获取redis的信息
        String lockjson = this.redisTemplate.opsForValue().get(key_prefix + orderToken);
        if(StringUtils.isEmpty(lockjson)){
            return;
        }
        List<SkuLockVo> lockVos = JSON.parseArray(lockjson, SkuLockVo.class);
        lockVos.forEach(skuLockVo -> {
            this.wareSkuDao.unlockStock(skuLockVo.getWareSkuId(),skuLockVo.getCount());
        });
    }
}

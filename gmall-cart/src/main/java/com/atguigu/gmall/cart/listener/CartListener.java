package com.atguigu.gmall.cart.listener;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.cart.frign.GmallPmsClient;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class CartListener {
    private static final String key_prefix = "cart:";
    private static final String price_prefix = "cart:price:";
    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "cart_price_queue",durable = "true"),
            exchange = @Exchange(value = "PMS_SPU_EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"item.update"}
    ))
    public void listener(Long spuId){

        List<SkuInfoEntity> infoEntities =
                this.pmsClient.querySkuBySpuId(spuId).getData();

        if(CollectionUtils.isEmpty(infoEntities)){
            return;
        }

        infoEntities.forEach(skuInfoEntity -> {
            this.redisTemplate.opsForValue().set(price_prefix+skuInfoEntity.getSkuId(),skuInfoEntity.getPrice().toString());
        });

    }






    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "Order-cart-queue",durable = "ture"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "ture",type = ExchangeTypes.TOPIC),
            key = {"cart.delete"}
    ))
    public void deleteCart(Map<String,Object> map){
        if(CollectionUtils.isEmpty(map)){
            return;
        }
        Long userId = (Long) map.get("userId");
        String skuIdstring = map.get("skuIds").toString();
        if(StringUtils.isEmpty(skuIdstring)){
            return;
        }

        List<String> skuIds = JSON.parseArray(skuIdstring, String.class);

        BoundHashOperations<String, Object, Object> hashOps
                = this.redisTemplate.boundHashOps(key_prefix + userId);
        hashOps.delete(skuIds.toArray());
    }

}

package com.atguigu.gmall.oms.listener;


import com.atguigu.gmall.oms.dao.OrderDao;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;

@Component
public class OrderListener {

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private AmqpTemplate amqpTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order-close-queue",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type= ExchangeTypes.TOPIC),
            key = {"oms.close","order.dead"}
    ))
    public void closeOrder(String orderToken){
        this.orderDao.closeOrder(orderToken);
        //关单 解锁库存
        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","wms.unlock",orderToken);
    }



    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "order-pay-queue",durable = "true"),
            exchange = @Exchange(value = "ORDER-EXCHANGE",ignoreDeclarationExceptions = "true",type = ExchangeTypes.TOPIC),
            key = {"order.pay"}
    ))
    public void paySuccess(String orderToken){

        int i = this.orderDao.payOrder(orderToken);
        if(i == 1){
            //减库存
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","stock.minus",orderToken);
            //给用户添加积分信息
            HashMap<String, Object> map = new HashMap<>();
            // TODO 根据订单编号查询用户信息
            map.put("userId",null);
            map.put("growh",null);
            map.put("intergration",null);
            this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","user.bounds",map);
        }

    }

}

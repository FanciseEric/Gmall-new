package com.atguigu.gmall.oms.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;

@Configuration
public class RabbitConfig {
    //普通交换机 ORDER-EXCHANGE

    //延时队列
    @Bean
    public Queue ttlQueue(){
        HashMap<String, Object> arguments = new HashMap<>();
        arguments.put("x-message-ttl",120000);//延迟队列名称  时间
        arguments.put("x-dead-letter-exchange","ORDER-EXCHANGE");//死信交换机绑定
        arguments.put("x-dead-letter-routing-key","order.dead");//routing key
        return new Queue("order-ttl-queue",true,false,false,arguments);
    }

    //延时队列绑定到交换机
    @Bean
    public Binding ttlBinding(){
        return new Binding("order-ttl-queue", Binding.DestinationType.QUEUE,
                "ORDER-EXCHANGE","order.ttl",null);
    }
//
//    //死信队列
//    @Bean
//    public Queue dlQueue(){
//        return new Queue("order-dead-queue",true,false,false,null);
//    }
}

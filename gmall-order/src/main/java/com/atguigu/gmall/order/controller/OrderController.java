package com.atguigu.gmall.order.controller;


import com.alipay.api.AlipayApiException;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.order.config.AlipayTemplate;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.vo.PayAsyncVo;
import com.atguigu.gmall.order.vo.PayVo;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RequestMapping("order")
@RestController
public class OrderController {


    @Autowired
    private OrderService orderService;

    @Autowired
    private AlipayTemplate alipayTemplate;

    @Autowired
    private AmqpTemplate amqpTemplate;
    @GetMapping("confirm")
    public Resp<OrderConfirmVo> confirm(){
        OrderConfirmVo confirmVo =  this.orderService.confirm();
        return Resp.ok(confirmVo);
    }


    @PostMapping("submit")
    public Resp<Object> submit(@RequestBody OrderSubmitVo submitVo){
        OrderEntity submit = this.orderService.submit(submitVo);

        try {
            PayVo payVo = new PayVo();
            payVo.setOut_trade_no(submit.getOrderSn());
            payVo.setTotal_amount(submit.getTotalAmount().toString());
            payVo.setSubject("商城支付平台");
            payVo.setBody("谷粒商城平台");
            String form = this.alipayTemplate.pay(payVo);
            System.out.println("支付宝返回："+form);
        } catch (AlipayApiException e) {
            e.printStackTrace();
        }
        return Resp.ok(null);
    }

    @PostMapping("pay/success")
    public Resp<Object> paySuccess(PayAsyncVo payAsyncVo){
        //修改订单状态
        this.amqpTemplate.convertAndSend("ORDER-EXCHANGE","order.pay",payAsyncVo.getOut_trade_no());

        return Resp.ok(payAsyncVo);
    }
}

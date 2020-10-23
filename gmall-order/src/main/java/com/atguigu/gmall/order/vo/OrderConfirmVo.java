package com.atguigu.gmall.order.vo;


import com.atguigu.gmall.oms.vo.OrderItemVo;
import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.util.List;

@Data
public class OrderConfirmVo {

    //收货地址
    private List<MemberReceiveAddressEntity> addresses;

    //送货清单
    private List<OrderItemVo> items;

    //积分
    private Integer bounds;

    //订单确认页唯一标志
    private String orderToken;

}

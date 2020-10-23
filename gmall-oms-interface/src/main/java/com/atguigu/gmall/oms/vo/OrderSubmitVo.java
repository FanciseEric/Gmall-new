package com.atguigu.gmall.oms.vo;


import com.atguigu.gmall.ums.entity.MemberReceiveAddressEntity;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderSubmitVo {

    private String orderToken;

    private BigDecimal totalPrice;

    private MemberReceiveAddressEntity address;//收货人信息

    private Integer payType;//支付方式

    private String deliveryCompany;//配送方式

    private List<OrderItemVo> items;//订单信息

    private Integer bounds;//积分信息

    private Long userId;
}

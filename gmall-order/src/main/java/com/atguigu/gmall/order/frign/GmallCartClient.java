package com.atguigu.gmall.order.frign;

import com.atguigu.gmall.cart.api.CartApi;
import com.atguigu.gmall.pms.api.GmallPmsApi;
import org.springframework.cloud.openfeign.FeignClient;

@FeignClient("cart-service")
public interface GmallCartClient extends CartApi {
}

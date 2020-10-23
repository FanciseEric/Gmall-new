package com.atguigu.gmall.cart.api;

import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.cart.entity.Cart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;


public interface CartApi {
    @GetMapping("cart/check/{userId}")
    public Resp<List<Cart>> queryCheckCarts(@PathVariable("userId")Long userId);
}

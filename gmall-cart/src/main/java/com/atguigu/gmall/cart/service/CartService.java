package com.atguigu.gmall.cart.service;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.UserInfo;
import com.atguigu.gmall.cart.entity.Cart;
import com.atguigu.gmall.cart.frign.GmallPmsClient;
import com.atguigu.gmall.cart.frign.GmallSmsClient;
import com.atguigu.gmall.cart.frign.GmallWmsClient;
import com.atguigu.gmall.cart.interceptor.LoginInterceptor;
import com.atguigu.gmall.pms.entity.SkuInfoEntity;
import com.atguigu.gmall.pms.entity.SkuSaleAttrValueEntity;
import com.atguigu.gmall.sms.vo.ItemSaleVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class CartService {

    private static final String key_prefix = "cart:";
    private static final String price_prefix = "cart:price:";
    @Autowired
    private GmallPmsClient pmsClient;

    @Autowired
    private GmallWmsClient wmsClient;

    @Autowired
    private GmallSmsClient smsClient;

    @Autowired
    private StringRedisTemplate redisTemplate;


    public void addCart(Cart cart) {
        //1:获取用户信息 判断登录状态
        String key = getKey();

        //2:获取该用户的购物车操作对象
        BoundHashOperations<String, Object, Object> hashOps
                = this.redisTemplate.boundHashOps(key);
        BigDecimal count = cart.getCount();
        //3:判断购物车中有没有该商品
        String skuId = cart.getSkuId().toString();
        if(hashOps.hasKey(skuId)){
            //有就更新数量
            String cartJson = hashOps.get(skuId).toString();
           cart =  JSON.parseObject(cartJson,Cart.class);
            cart.setCount(cart.getCount().add(count));

        }else {
            //没有就新增
            SkuInfoEntity skuInfoEntity
                    = this.pmsClient.querySkuById(cart.getSkuId()).getData();
            cart.setImage(skuInfoEntity.getSkuDefaultImg());
            cart.setPrice(skuInfoEntity.getPrice());
            cart.setTitle(skuInfoEntity.getSkuTitle());
            cart.setCheck(true);
            List<ItemSaleVo> itemSaleVos = this.smsClient.queryItemSaleBySkuId(cart.getSkuId()).getData();
            cart.setSales(itemSaleVos);

            List<SkuSaleAttrValueEntity> attrValueEntities
                    = this.pmsClient.querySaleAttrBySkuId(cart.getSkuId()).getData();
            cart.setSaleAttrs(attrValueEntities);

            this.redisTemplate.opsForValue().set(price_prefix+skuId,skuInfoEntity.getPrice().toString());

        }

        hashOps.put(skuId,JSON.toJSONString(cart));

    }

    public List<Cart> queryCarts() {
        //1:获取登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();

        //2:以userkey作为key查询未登录的购物车
        String unLoginKey = key_prefix+userInfo.getUserKey();
        BoundHashOperations<String, Object, Object> hashOps
                = this.redisTemplate.boundHashOps(unLoginKey);
        List<Object> cartJsons = hashOps.values();
        List<Cart> unLoginCarts = null;
        if(!CollectionUtils.isEmpty(cartJsons)){
            //未登录购物车信息
            unLoginCarts = cartJsons.stream().map(cartJson -> {
                Cart cart = JSON.parseObject(cartJson.toString(), Cart.class);
                String currentPrice = this.redisTemplate.opsForValue().get(price_prefix + cart.getSkuId());
                cart.setCurrentPrice(new BigDecimal(currentPrice));
                return cart;
            }).collect(Collectors.toList());
        }
        //3:判断登录状态,未登录直接返回
        if(userInfo.getUserId() == null){
            return unLoginCarts;
        }
        //4:登录情况下合并购物车
        String loginKey = key_prefix+userInfo.getUserId();
        BoundHashOperations<String, Object, Object>
                loginOps = this.redisTemplate.boundHashOps(loginKey);
        if(!CollectionUtils.isEmpty(unLoginCarts)){
            //合并
            unLoginCarts.forEach(cart -> {
                //商品是否存在购物车，存在就增加数量 不存在就增加这个商品
                if(loginOps.hasKey(cart.getSkuId().toString())){
                    //存在更新数量
                    BigDecimal count = cart.getCount();
                    String loginCartJs = loginOps.get(cart.getSkuId().toString()).toString();
                    cart = JSON.parseObject(loginCartJs,Cart.class);
                    cart.setCount(cart.getCount().add(count));

                }

                //不存在  新增
                loginOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
            });
        }


        this.redisTemplate.delete(key_prefix+userInfo.getUserKey());
        //查询
        List<Object> loginValues = loginOps.values();
        if(!CollectionUtils.isEmpty(loginValues)){
           return loginValues.stream().map(loginCarts ->{
               Cart cart = JSON.parseObject(loginCarts.toString(), Cart.class);
               String currentPrice = this.redisTemplate.opsForValue().get(price_prefix + cart.getSkuId());
               cart.setCurrentPrice(new BigDecimal(currentPrice));
               return cart;
           }).collect(Collectors.toList());
        }
        return null;
    }

    //修改购物车数量
    public void updateNum(Cart cart) {
        //1:获取用户登录信息
        String key = getKey();

        //3获取购物车操作对象
        BoundHashOperations<String, Object, Object> boundHashOps =
                this.redisTemplate.boundHashOps(key);

        String cartJson = boundHashOps.get(cart.getSkuId().toString()).toString();
        BigDecimal count = cart.getCount();
        if(StringUtils.isNotBlank(cartJson)){
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCount(count);
            boundHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }


    public void updateCheck(Cart cart) {
        String key = getKey();

        //3获取购物车操作对象
        BoundHashOperations<String, Object, Object> boundHashOps =
                this.redisTemplate.boundHashOps(key);


        String cartJson = boundHashOps.get(cart.getSkuId().toString()).toString();
        Boolean check =  cart.getCheck();
        if(StringUtils.isNotBlank(cartJson)){
            cart = JSON.parseObject(cartJson, Cart.class);
            cart.setCheck(check);
            boundHashOps.put(cart.getSkuId().toString(),JSON.toJSONString(cart));
        }
    }

    private String getKey() {
        //1:获取用户登录信息
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        //2:判断登录状态，组装key
        String key = key_prefix;
        if (userInfo.getUserId() == null) {
            key += userInfo.getUserKey();
        } else {
            key += userInfo.getUserId();
        }
        return key;
    }

    public void deleteCart(Long skuId) {

        String key = getKey();

        BoundHashOperations<String, Object, Object> hashOps = this.redisTemplate.boundHashOps(key);
        hashOps.delete(skuId.toString());
    }

    public List<Cart> queryCheckCarts(Long userId) {
        String key = key_prefix+userId;
        BoundHashOperations<String, Object, Object> boundHashOps =
                this.redisTemplate.boundHashOps(key);
        List<Object> cartJsons = boundHashOps.values();
        if(CollectionUtils.isEmpty(cartJsons)){
            return null;
        }

       return cartJsons.stream().map(cartJson ->JSON.parseObject(cartJson.toString(),Cart.class)).filter(cart -> cart.getCheck()).collect(Collectors.toList());

    }
}

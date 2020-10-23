package com.atguigu.gmall.wms.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.gmall.wms.dao.WareSkuDao;
import com.atguigu.gmall.wms.entity.WareSkuEntity;
import com.atguigu.gmall.wms.service.WareSkuService;
import com.atguigu.gmall.wms.vo.SkuLockVo;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service("wareSkuService")
public class WareSkuServiceImpl extends ServiceImpl<WareSkuDao, WareSkuEntity> implements WareSkuService {


    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private WareSkuDao wareSkuDao;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String key_prefix =  "wms:lock:";

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<WareSkuEntity> page = this.page(
                new Query<WareSkuEntity>().getPage(params),
                new QueryWrapper<WareSkuEntity>()
        );

        return new PageVo(page);
    }

    @Transactional
    @Override
    public List<SkuLockVo> checkAndLock(List<SkuLockVo> lockVOS) {

        if (CollectionUtils.isEmpty(lockVOS)) {
            return null;
        }

        lockVOS.forEach(lockVO -> {
            // 每一个商品验库存并锁库存
            this.checkLock(lockVO);
       });

        // 如果有一个商品锁定失败了，所有已经成功锁定的商品要解库存
        List<SkuLockVo> successLockVO = lockVOS.stream().filter(SkuLockVo::getLock).collect(Collectors.toList());
        List<SkuLockVo> errorLockVO = lockVOS.stream().filter(skuLockVO -> !skuLockVO.getLock()).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(errorLockVO)) {
            successLockVO.forEach(lockVO -> {
                this.wareSkuDao.unlockStock(lockVO.getWareSkuId(), lockVO.getCount());
            });
            return lockVOS;
        }
        //锁定库存信息放入redis
        String orderToken = lockVOS.get(0).getOrderToken();
        this.redisTemplate.opsForValue().set(key_prefix+orderToken, JSON.toJSONString(lockVOS));
        return null;
    }


    private void checkLock(SkuLockVo skuLockVO){
        RLock fairLock = this.redissonClient.getFairLock("lock:" + skuLockVO.getSkuId());
        fairLock.lock();
        // 验库存
        List<WareSkuEntity> wareSkuEntities = this.wareSkuDao.checkStock(skuLockVO.getSkuId(), skuLockVO.getCount());
        if (CollectionUtils.isEmpty(wareSkuEntities)) {
            skuLockVO.setLock(false); // 库存不足，锁定失败
            fairLock.unlock();
            return;
        }
        // 锁库存。一般会根据运输距离，就近调配。这里就锁定第一个仓库的库存
        if(this.wareSkuDao.lockStock(wareSkuEntities.get(0).getId(), skuLockVO.getCount()) == 1){
            skuLockVO.setLock(true); // 锁定成功
            skuLockVO.setWareSkuId(wareSkuEntities.get(0).getId());
        } else {
            skuLockVO.setLock(false);
        }
        fairLock.unlock();
    }
}
package com.atguigu.gmall.sms.dao;

import com.atguigu.gmall.sms.entity.SeckillSessionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 秒杀活动场次
 * 
 * @author liqingbi
 * @email 1130274947@qq.com
 * @date 2020-08-09 13:35:33
 */
@Mapper
public interface SeckillSessionDao extends BaseMapper<SeckillSessionEntity> {
	
}

package com.atguigu.gmall.pms.service;

import com.atguigu.gmall.pms.vo.AttrGroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;

import java.util.List;


/**
 * 属性分组
 *
 * @author liqingbi
 * @email 1130274947@qq.com
 * @date 2020-08-09 10:03:36
 */
public interface AttrGroupService extends IService<AttrGroupEntity> {

    PageVo queryPage(QueryCondition params);

    PageVo queryGroupByCid(Long cid, QueryCondition queryCondition);

    AttrGroupVo queryGroupVoById(Long id);

    List<AttrGroupVo> queryGroupWithAttrByCid(Long catId);

    List<ItemGroupVo> queryItemGroupsBySpuIdAndCid(Long spuId, Long cid);
}


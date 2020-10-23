package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.dao.AttrAttrgroupRelationDao;
import com.atguigu.gmall.pms.dao.AttrDao;
import com.atguigu.gmall.pms.dao.ProductAttrValueDao;
import com.atguigu.gmall.pms.entity.AttrAttrgroupRelationEntity;
import com.atguigu.gmall.pms.entity.AttrEntity;
import com.atguigu.gmall.pms.entity.ProductAttrValueEntity;
import com.atguigu.gmall.pms.vo.AttrGroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.AttrGroupDao;
import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;
import org.springframework.util.CollectionUtils;


@Service("attrGroupService")
public class AttrGroupServiceImpl extends ServiceImpl<AttrGroupDao, AttrGroupEntity> implements AttrGroupService {

    @Autowired
    private AttrAttrgroupRelationDao relationDao;

    @Autowired
    private AttrDao attrDao;


    @Autowired
    private ProductAttrValueDao productAttrValueDao;


    @Override
    public List<ItemGroupVo> queryItemGroupsBySpuIdAndCid(Long spuId, Long cid) {
        //1：根据cid查询分组
        List<AttrGroupEntity> attrGroupEntitys = this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", cid));

        if(attrGroupEntitys ==null){
            return new ArrayList<>();
        }

        //2：查询组下的规格参数
        return attrGroupEntitys.stream().map(attrGroupEntity -> {
            ItemGroupVo itemGroupVo = new ItemGroupVo();
            itemGroupVo.setGroupId(attrGroupEntity.getAttrGroupId());
            itemGroupVo.setGroupName(attrGroupEntity.getAttrGroupName());
            List<AttrAttrgroupRelationEntity> relationEntities
                    = this.relationDao.selectList(
                            new QueryWrapper<AttrAttrgroupRelationEntity>()
                                    .eq("attr_group_id", attrGroupEntity.getAttrGroupId()));
            if(!CollectionUtils.isEmpty(relationEntities)){
                List<Long> attrIds = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
                //：attrId结合spuId查询规格参数对应的值
                List<ProductAttrValueEntity> attrValueEntities
                        = this.productAttrValueDao.selectList(
                                new QueryWrapper<ProductAttrValueEntity>()
                                        .eq("spu_id", spuId)
                                        .in("attr_id", attrIds));
                itemGroupVo.setBaseAttrValues(attrValueEntities);
            }
            return itemGroupVo;

        }).collect(Collectors.toList());
    }
    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(params),
                new QueryWrapper<AttrGroupEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public PageVo queryGroupByCid(Long cid, QueryCondition queryCondition) {
        IPage<AttrGroupEntity> page = this.page(
                new Query<AttrGroupEntity>().getPage(queryCondition),
                new QueryWrapper<AttrGroupEntity>().eq("catelog_id",cid)
        );

        return new PageVo(page);
    }

    @Override
    public AttrGroupVo queryGroupVoById(Long id) {
        AttrGroupVo attrGroupVo = new AttrGroupVo();
        //1:根据id查询分组
        AttrGroupEntity groupEntity = this.getById(id);
        BeanUtils.copyProperties(groupEntity,attrGroupVo);
        //2：根据分组的id查询中间表
        List<AttrAttrgroupRelationEntity> relationEntities
                = this.relationDao.selectList(new QueryWrapper<AttrAttrgroupRelationEntity>().eq("attr_group_id", id));
        if(CollectionUtils.isEmpty(relationEntities)){
            return attrGroupVo;
        }
        attrGroupVo.setRelations(relationEntities);
        //3：搜集中间表的attrId集合  查询规格参数
//        List<Long> ids = new ArrayList<>();
//        for(AttrAttrgroupRelationEntity relationEntity:relationEntities){
//            ids.add(relationEntity.getAttrId());
//        }
        List<Long> ids = relationEntities.stream().map(AttrAttrgroupRelationEntity::getAttrId).collect(Collectors.toList());
        List<AttrEntity> attrEntities = this.attrDao.selectBatchIds(ids);
        attrGroupVo.setAttrEntities(attrEntities);
        return attrGroupVo;
    }

    @Override
    public List<AttrGroupVo> queryGroupWithAttrByCid(Long catId) {
       //1:根据分类id 查询分组
        List<AttrGroupEntity> groupEntities =
                this.list(new QueryWrapper<AttrGroupEntity>().eq("catelog_id", catId));
        if(CollectionUtils.isEmpty(groupEntities)){
            return null;
        }
        return groupEntities.stream().map(attrGroupEntity -> {
            AttrGroupVo attrGroupVo = this.queryGroupVoById(attrGroupEntity.getAttrGroupId());
            return attrGroupVo;
        }).collect(Collectors.toList());

    }



}
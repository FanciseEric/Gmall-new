package com.atguigu.gmall.pms.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.vo.AttrGroupVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.atguigu.gmall.pms.entity.AttrGroupEntity;
import com.atguigu.gmall.pms.service.AttrGroupService;




/**
 * 属性分组
 *
 * @author liqingbi
 * @email 1130274947@qq.com
 * @date 2020-08-09 10:03:36
 */
@Api(tags = "属性分组 管理")
@RestController
@RequestMapping("pms/attrgroup")
public class AttrGroupController {
    @Autowired
    private AttrGroupService attrGroupService;


    @GetMapping("withattrvalues")
    public Resp<List<ItemGroupVo>> queryItemGroupsBySpuIdAndCid(
            @RequestParam("spuId")Long spuId,
            @RequestParam("cid")Long cid
    ){
        List<ItemGroupVo> itemGroupVos = this.attrGroupService.queryItemGroupsBySpuIdAndCid(spuId,cid);

        return Resp.ok(itemGroupVos!=null ? itemGroupVos : new ArrayList<>());
    }



    @GetMapping("/withattrs/cat/{catId}")
    public  Resp<List<AttrGroupVo>> queryGroupWithAttrByCid(@PathVariable("catId")Long catId){
        List<AttrGroupVo> groupVos = this.attrGroupService.queryGroupWithAttrByCid(catId);
        return Resp.ok(groupVos);
    }




    @GetMapping("/withattr/{gid}")
    public Resp<AttrGroupVo> queryGroupVoById(@PathVariable("gid")Long id){
       AttrGroupVo attrGroupVo =  this.attrGroupService.queryGroupVoById(id);
       return Resp.ok(attrGroupVo);
    }




    /**
     * 查询三级分类下的分组
     * @param cid
     * @param queryCondition
     * @return
     */
    @GetMapping("{catId}")
    public Resp<PageVo> queryGroupByCid(@PathVariable("catId")Long cid,
                                        QueryCondition queryCondition){

        PageVo pageVo = this.attrGroupService.queryGroupByCid(cid,queryCondition);
        return Resp.ok(pageVo);
    }

    /**
     * 列表
     */
    @ApiOperation("分页查询(排序)")
    @GetMapping("/list")
    @PreAuthorize("hasAuthority('pms:attrgroup:list')")
    public Resp<PageVo> list(QueryCondition queryCondition) {
        PageVo page = attrGroupService.queryPage(queryCondition);

        return Resp.ok(page);
    }


    /**
     * 信息
     */
    @ApiOperation("详情查询")
    @GetMapping("/info/{attrGroupId}")
    @PreAuthorize("hasAuthority('pms:attrgroup:info')")
    public Resp<AttrGroupEntity> info(@PathVariable("attrGroupId") Long attrGroupId){
		AttrGroupEntity attrGroup = attrGroupService.getById(attrGroupId);

        return Resp.ok(attrGroup);
    }

    /**
     * 保存
     */
    @ApiOperation("保存")
    @PostMapping("/save")
    @PreAuthorize("hasAuthority('pms:attrgroup:save')")
    public Resp<Object> save(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.save(attrGroup);

        return Resp.ok(null);
    }

    /**
     * 修改
     */
    @ApiOperation("修改")
    @PostMapping("/update")
    @PreAuthorize("hasAuthority('pms:attrgroup:update')")
    public Resp<Object> update(@RequestBody AttrGroupEntity attrGroup){
		attrGroupService.updateById(attrGroup);

        return Resp.ok(null);
    }

    /**
     * 删除
     */
    @ApiOperation("删除")
    @PostMapping("/delete")
    @PreAuthorize("hasAuthority('pms:attrgroup:delete')")
    public Resp<Object> delete(@RequestBody Long[] attrGroupIds){
		attrGroupService.removeByIds(Arrays.asList(attrGroupIds));

        return Resp.ok(null);
    }

}

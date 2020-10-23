package com.atguigu.gmall.pms.api;

import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.pms.entity.*;
import com.atguigu.gmall.pms.vo.AttrGroupVo;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.vo.ItemGroupVo;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.List;

public interface GmallPmsApi {
    /**
     * 分页查询spu下的sku接口
     * @param queryCondition
     * @return
     */
    @PostMapping("pms/spuinfo/page")
    public Resp<List<SpuInfoEntity>> querySpuPage(@RequestBody QueryCondition queryCondition);

    /**
     * 根据spuId 查询 sku集合
     * @param spuId
     * @return
     */
    @GetMapping("pms/skuinfo/{spuId}")
    public Resp<List<SkuInfoEntity>> querySkuBySpuId(@PathVariable("spuId")Long spuId);


    /**
     * 根据品牌id查询品牌详情
     * @param brandId
     * @return
     */
    @GetMapping("pms/brand/info/{brandId}")
    public Resp<BrandEntity> queryBrandById(@PathVariable("brandId") Long brandId);


    /**
     * 根据分类Id 查询分类详情
     * @param catId
     * @return
     */
    @GetMapping("pms/category/info/{catId}")
    public Resp<CategoryEntity> queryCategoryById(@PathVariable("catId") Long catId);


    /**
     * 根据spuId查询检索的规格参数及值
     * @param spuId
     * @return
     */
    @GetMapping("pms/productattrvalue/{spuId}")
    public Resp<List<ProductAttrValueEntity>> queryAttrValueBySpuId(@PathVariable("spuId")Long spuId);


    /**
     * 查询一级分类
     * @return
     */
    @GetMapping("pms/category")
    public Resp<List<CategoryEntity>> queryCategoriesByLevelOrPid(@RequestParam(value = "level",defaultValue = "0")Integer level,
                                                                  @RequestParam(value = "parentCid",required = false)Long pid);


    /**
     * 查询一级分类下的二级 三级分类
     * @param pid
     * @return
     */
    @GetMapping("pms/category/{pid}")
    public Resp<List<CategoryVo>> queryCategoryWithSubByPid(@PathVariable("pid")Long pid);

    /**
     * 根据skuId查询详情
     * @param skuId
     * @return
     */
    @GetMapping("pms/skuinfo/info/{skuId}")
    public Resp<SkuInfoEntity> querySkuById(@PathVariable("skuId") Long skuId);


    /**
     * 根据spuId查询详情
     * @param id
     * @return
     */
    @GetMapping("pms/spuinfo/info/{id}")
    public Resp<SpuInfoEntity> querySpuById(@PathVariable("id") Long id);

    @ApiOperation("详情查询")
    @GetMapping("pms/spuinfodesc/info/{spuId}")
    public Resp<SpuInfoDescEntity> querySpuDescBySpuId(@PathVariable("spuId") Long spuId);


    @GetMapping("pms/attrgroup/withattrs/cat/{catId}")
    public  Resp<List<AttrGroupVo>> queryGroupWithAttrByCid(@PathVariable("catId")Long catId);


    @GetMapping("pms/skuimages/{skuId}")
    public Resp<List<SkuImagesEntity>> querySkuyImagesBySkuId(@PathVariable("skuId")Long skuId);



    @GetMapping("pms/skusaleattrvalue/{spuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySaleAttrBySpuId(@PathVariable("spuId")Long spuId);


    @GetMapping("pms/attrgroup/withattrvalues")
    public Resp<List<ItemGroupVo>> queryItemGroupsBySpuIdAndCid(
            @RequestParam("spuId")Long spuId,
            @RequestParam("cid")Long cid
    );


    @GetMapping("pms/skusaleattrvalue/sku/{skuId}")
    public Resp<List<SkuSaleAttrValueEntity>> querySaleAttrBySkuId(@PathVariable("skuId")Long skuId);
}

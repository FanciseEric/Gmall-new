package com.atguigu.gmall.index.controller;


import com.atguigu.core.bean.Resp;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/index")
public class IndexController {


    @Autowired
    private IndexService indexService;

    @GetMapping("/cates")
    public Resp<List<CategoryEntity>> queryLvlCategory(){

        List<CategoryEntity> categoryEntities =  this.indexService.queryLvlCategory();
        return Resp.ok(categoryEntities);
    }


    @GetMapping("/cates/{pid}")
    public Resp<List<CategoryVo>> queryLv2WithSubByPid(@PathVariable("pid")Long pid){
        List<CategoryVo> categoryVos = this.indexService.queryLv2WithSubByPid(pid);
        return Resp.ok(categoryVos);
    }

}

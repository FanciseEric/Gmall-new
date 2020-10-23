package com.atguigu.gmall.pms.service.impl;

import com.atguigu.gmall.pms.vo.CategoryVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;

import com.atguigu.gmall.pms.dao.CategoryDao;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import com.atguigu.gmall.pms.service.CategoryService;
import org.springframework.util.CollectionUtils;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

    @Autowired
    private CategoryDao categoryDao;


    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public List<CategoryEntity> queryCategoriesByLevelOrPid(Integer level, Long pid) {
        QueryWrapper<CategoryEntity> wrapper = new QueryWrapper<>();
        //1:判断层级是否为0
        if(level != 0){
            //第一个是列明，第二个是匹配参数
            wrapper.eq("cat_level",level);
        }
        //2:判断pid是否为空
        if(pid != null){
            //第一个是列明，第二个是匹配参数
            wrapper.eq("parent_cid",pid);
        }

        return this.list(wrapper);
    }

    @Override
    public List<CategoryVo> queryCategoryWithSubByPid(Long pid) {
        List<CategoryVo> categoryVos =  this.categoryDao.queryCategoryWithSubByPid(pid);
        if(!CollectionUtils.isEmpty(categoryVos)){
            return categoryVos;
        }
        return new ArrayList<>();
    }

}
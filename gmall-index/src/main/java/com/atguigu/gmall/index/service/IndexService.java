package com.atguigu.gmall.index.service;

import com.atguigu.gmall.pms.vo.CategoryVo;
import com.atguigu.gmall.pms.entity.CategoryEntity;

import java.util.List;

public interface IndexService {
    List<CategoryEntity> queryLvlCategory();

    List<CategoryVo> queryLv2WithSubByPid(Long pid);
}

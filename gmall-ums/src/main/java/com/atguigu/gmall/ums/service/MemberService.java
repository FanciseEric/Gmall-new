package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.QueryCondition;


/**
 * 会员
 *
 * @author liqingbi
 * @email 1130274947@qq.com
 * @date 2020-10-09 15:31:47
 */
public interface MemberService extends IService<MemberEntity> {

    PageVo queryPage(QueryCondition params);

    Boolean checkData(String data, Integer type);

    Boolean register(MemberEntity memberEntity, String code);

    MemberEntity queryUserByPad(String username, String password);

}


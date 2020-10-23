package com.atguigu.gmall.ums.service.impl;

import com.atguigu.core.bean.PageVo;
import com.atguigu.core.bean.Query;
import com.atguigu.core.bean.QueryCondition;
import com.atguigu.core.exception.MemberException;
import com.atguigu.gmall.ums.dao.MemberDao;
import com.atguigu.gmall.ums.entity.MemberEntity;
import com.atguigu.gmall.ums.service.MemberService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import net.bytebuddy.implementation.bytecode.Throw;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Override
    public PageVo queryPage(QueryCondition params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageVo(page);
    }

    @Override
    public MemberEntity queryUserByPad(String username, String password) {
        MemberEntity memberEntity = this.getOne(new QueryWrapper<MemberEntity>().eq("username", username));
        if(memberEntity == null){
            throw  new MemberException("用户名不存在！");
        }
        //获取盐
        String salt = memberEntity.getSalt();
        password = DigestUtils.md5Hex(password + salt);


        //比较
        boolean equals = StringUtils.equals(password, memberEntity.getPassword());
        if(equals== false){
            throw new MemberException("密码输入错误");
        }
        return memberEntity;
    }


    @Override
    public Boolean register(MemberEntity memberEntity, String code) {

        //校验验证码

        try {
            //生成盐
            String salt = UUID.randomUUID().toString().substring(0, 6);
            memberEntity.setSalt(salt);

            //加密
            String password = DigestUtils.md5Hex(memberEntity.getPassword() + salt);
            memberEntity.setPassword(password);

            //新增用户
            memberEntity.setCreateTime(new Date());
            memberEntity.setIntegration(100);
            memberEntity.setGrowth(100);
            memberEntity.setStatus(1);
            memberEntity.setLevelId(0L);


            return this.save(memberEntity)?true:false;
        }catch (Exception e){
            return false;
        }
    }



    @Override
    public Boolean checkData(String data, Integer type) {
        QueryWrapper<MemberEntity> wrapper = new QueryWrapper<>();
        switch (type){
            case 1:
                wrapper.eq("username",data);
                break;
            case 2:
                wrapper.eq("mobile",data);
                break;
            case 3:
                wrapper.eq("email",data);
                break;
            default:
                break;
        }

        return this.count(wrapper) == 0;
    }


}
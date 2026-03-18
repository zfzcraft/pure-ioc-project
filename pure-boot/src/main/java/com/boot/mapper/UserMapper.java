package com.boot.mapper;

import org.apache.ibatis.annotations.Mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.boot.entity.User;
@Mapper
public interface UserMapper extends BaseMapper<User>{

}

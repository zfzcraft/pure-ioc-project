package com.boot.service.impl;

import com.boot.entity.User;
import com.boot.mapper.UserMapper;
import com.boot.service.UserService;

import cn.zfzcraft.pureioc.annotations.Component;
@Component
public class UserServiceImpl implements UserService{

	UserMapper userMapper;

	public UserServiceImpl(UserMapper userMapper) {
		super();
		this.userMapper = userMapper;
	}

	@Override
	public User selectById(int id) {
		return userMapper.selectById(id);
	}
	
	
}

package com.boot.controller;

import com.boot.entity.User;
import com.boot.service.UserService;

import netty.http.mvc.GetMapping;
import netty.http.mvc.RequestMapping;
import netty.http.mvc.RestController;

@RestController
@RequestMapping("/user")
public class UserController {

	
	UserService userService;
	
	public UserController(UserService userService) {
		super();
		this.userService = userService;
	}

	@GetMapping("/get")
	public User get() {
		return userService.selectById(1);
	}
}

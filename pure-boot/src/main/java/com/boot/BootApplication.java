package com.boot;

import cn.zfzcraft.pureioc.annotations.Bootstrap;
import cn.zfzcraft.pureioc.core.BootstrapApplication;
@Bootstrap
public class BootApplication {

	public static void main(String[] args) {
		long begin = System.currentTimeMillis();
		BootstrapApplication.run(args, BootApplication.class);
		long end = System.currentTimeMillis();
		System.out.println("启动时间："+(end-begin));
	}
}

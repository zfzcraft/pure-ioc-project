package cn.zfzcraft.tx;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import javax.sql.DataSource;

public class TransactionableInvocationHandler implements InvocationHandler {

	public TransactionableInvocationHandler(DataSource dataSource) {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		// TODO Auto-generated method stub
		return null;
	}

}

package netty.http.mvc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fastjson2.JSONObject;

import cn.zfzcraft.pureioc.core.ApplicationContext;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.util.CharsetUtil;
import io.netty.channel.ChannelHandler.Sharable;;
@Sharable
public class HttpServerHandler extends SimpleChannelInboundHandler<FullHttpRequest> {

	private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=UTF-8";
	
	private ApplicationContext applicationContext;
	
	private final Map<String, Handler> handlerMap = new ConcurrentHashMap<>();

	public HttpServerHandler(ApplicationContext applicationContext) {
		super();
		this.applicationContext = applicationContext;
		init();
	}

	private void init() {
		for (Class<?> clazz : applicationContext.getBeanClasses()) {
			if (clazz.isAnnotationPresent(RestController.class)) {
				RequestMapping requestMapping = clazz.getAnnotation(RequestMapping.class);
				String classUrl = "";
				if (requestMapping!=null) {
					classUrl = requestMapping.value();
				}
				// 遍历所有方法，找@MiniGetMapping注解
				for (Method method : clazz.getDeclaredMethods()) {
					if (method.isAnnotationPresent(GetMapping.class)) {
						GetMapping mapping = method.getAnnotation(GetMapping.class);
						String methodUrl = mapping.value();
						String url = classUrl+methodUrl;
						// 存入路由表：URL -> 处理器（对象+方法）
						handlerMap.put(url, new Handler(clazz, method));
					}
				}
			}
		}
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request)
			throws IllegalAccessException, InvocationTargetException {
		System.out.println("收到请求：" + request.uri());
		// 1. 获取请求信息
		String uri = request.uri();
		if (uri.endsWith("ico")) {
			ctx.writeAndFlush("");
		}else {
			String path = uri.split("\\?")[0];
			Handler handler = handlerMap.get(path);
			Object controller = getController(handler);

			Object result = handler.getMethod().invoke(controller);
			String content = JSONObject.toJSONString(result);
			FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK,
					Unpooled.copiedBuffer(content, CharsetUtil.UTF_8));

			// 设置头
			response.headers()
	        .set(HttpHeaderNames.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
	        .set(HttpHeaderNames.CONTENT_LENGTH, response.content().readableBytes());

			// 3. 写出并关闭
			ctx.writeAndFlush(response);
		}
		
	}

	private Object getController(Handler handler) {
		Object controller = handler.getController();
		if (controller == null) {
			synchronized (this) {
				if (controller == null) {
					controller = applicationContext.getBean(handler.getControllerClass());
					handler.setController(controller);
					return controller;
				}
			}
		}
		return controller;
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		cause.printStackTrace();
		ctx.close();
	}
}

package netty.http.mvc;

import java.util.concurrent.locks.LockSupport;

import cn.zfzcraft.pureioc.annotations.Component;
import cn.zfzcraft.pureioc.annotations.Eager;
import cn.zfzcraft.pureioc.core.ApplicationContext;
import cn.zfzcraft.pureioc.core.DisposableBean;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;

@SuppressWarnings("deprecation")
@Component
@Eager
public class NettyHttpServer implements DisposableBean{
	
	EventLoopGroup bossGroup = new NioEventLoopGroup(1);
	EventLoopGroup workerGroup = new NioEventLoopGroup();

	
	private HttpProperties httpProperties;
	
	HttpServerHandler httpServerHandler;

	public NettyHttpServer(ApplicationContext applicationContext, HttpProperties httpProperties)
			throws InterruptedException {
		super();
		
		this.httpProperties = httpProperties;
		httpServerHandler = new HttpServerHandler(applicationContext);
		start();
	}

	public void start() throws InterruptedException {
		try {
			ServerBootstrap b = new ServerBootstrap();
			b.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class).option(ChannelOption.SO_BACKLOG, 100)
					.childHandler(new ChannelInitializer<SocketChannel>() {
						@Override
						public void initChannel(SocketChannel ch) {
							ch.pipeline().addLast(new HttpServerCodec());
	                         ch.pipeline().addLast(new HttpObjectAggregator(1024*1024));
									// 自己的业务处理器
	                         ch.pipeline()	.addLast(httpServerHandler);
						}
					});

			ChannelFuture f = b.bind(httpProperties.getPort()).sync();
			System.out.println("Netty HTTP 服务启动：http://127.0.0.1:" + httpProperties.getPort());
			Thread thread = new Thread(() -> {
				try {
					f.channel().closeFuture().sync();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				while (true) {
					LockSupport.park();
				}
			});
			thread.setDaemon(false);
			thread.start();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void destroy() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
		
	}

}

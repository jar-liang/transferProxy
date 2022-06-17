package me.jar.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.jar.channel.ProxyChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description
 * @Date 2021/4/27-21:50
 */
public class ProxyHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class);
//    private static final ReentrantLock LOCK = new ReentrantLock();

    private Channel clientChannel = null;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
//        LOCK.lock();
//        try {
//            if (ProxyChannel.proxyChannel == null || !ProxyChannel.proxyChannel.isActive()) {
//                ProxyChannel.proxyChannel = ctx.channel();
//                if (ProxyChannel.clientChannel != null && ProxyChannel.clientChannel.isActive()) {
//                    ProxyChannel.clientChannel.close();
//                }
//            }
//        } finally {
//            LOCK.unlock();
//        }

        if (clientChannel != null && clientChannel.isActive()) {
            clientChannel.writeAndFlush(msg);
        } else {
            EventLoopGroup workGroup = new NioEventLoopGroup(1);
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(workGroup).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ChannelPipeline pipeline = ch.pipeline();
                    pipeline.addLast("clientHandler", new ClientHandler(ctx.channel()));
                }
            });
            String host = "192.168.0.101";
            int port = 3389;
            bootstrap.connect(host, port)
                    .addListener((ChannelFutureListener) connectFuture -> {
                        if (connectFuture.isSuccess()) {
                            LOGGER.info(">>>Connect far server successfully.");
//                            ReentrantLock reentrantLock = new ReentrantLock();
//                            reentrantLock.lock();
//                            try {
//                                if (ProxyChannel.clientChannel == null || !ProxyChannel.clientChannel.isActive()) {
//                                    ProxyChannel.clientChannel = connectFuture.channel();
//                                }
//                            } finally {
//                                reentrantLock.unlock();
//                            }
                            clientChannel = connectFuture.channel();
                            connectFuture.channel().writeAndFlush(msg);
                        } else {
                            LOGGER.error("===Failed to connect to far server! host: " + host + " , port: " + port);
                        }
                    });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.debug("===Client disconnected.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===ConnectFarHandler has caught exception, cause: {}", cause.getMessage());
        ctx.close();
    }
}

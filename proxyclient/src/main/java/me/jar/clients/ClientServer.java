package me.jar.clients;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import me.jar.channel.ProxyChannel;
import me.jar.handler.ProxyHandler;
import me.jar.utils.NettyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReentrantLock;


/**
 * @Description
 * @Date 2021/4/27-21:31
 */
public class ClientServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientServer.class);

    private final int port;

    public ClientServer(int port) {
        this.port = port;
    }

    public void run() {
//        EventLoopGroup workGroup = new NioEventLoopGroup(1);
//        Bootstrap bootstrap = new Bootstrap();
//        bootstrap.group(workGroup).channel(NioSocketChannel.class)
//                .option(ChannelOption.SO_KEEPALIVE, true)
//                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).handler(new ChannelInitializer<SocketChannel>() {
//            @Override
//            protected void initChannel(SocketChannel ch) {
//                ChannelPipeline pipeline = ch.pipeline();
//                pipeline.addLast("proxyHandler", new ProxyHandler());
//            }
//        });
//        String host = "127.0.0.1";
//        int port = 13333;
//        bootstrap.connect(host, port)
//                .addListener((ChannelFutureListener) connectFuture -> {
//                    if (connectFuture.isSuccess()) {
//                        LOGGER.info(">>>Connect far server successfully.");
//                        ReentrantLock reentrantLock = new ReentrantLock();
//                        reentrantLock.lock();
//                        try {
//                            if (ProxyChannel.proxyChannel == null || !ProxyChannel.proxyChannel.isActive()) {
//                                ProxyChannel.proxyChannel = connectFuture.channel();
//                            }
//                        } finally {
//                            reentrantLock.unlock();
//                        }
//                        connectFuture.channel().writeAndFlush(Unpooled.copiedBuffer("Connect", StandardCharsets.UTF_8));
//                    } else {
//                        LOGGER.error("===Failed to connect to far server! host: " + host + " , port: " + port);
//                    }
//                });

        ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ch.pipeline().addLast("clientHandler", new ProxyHandler());
            }
        };
        NettyUtil.starServer(port, channelInitializer);
    }

    public static void main(String[] args) {
//        if (ProxyConstants.PROPERTY.containsKey(ProxyConstants.KEY_NAME_PORT)) {
//            String port = ProxyConstants.PROPERTY.get(ProxyConstants.KEY_NAME_PORT);
//            try {
//                int portNum = Integer.parseInt(port.trim());
//                new ClientServer(portNum).run();
//            } catch (NumberFormatException e) {
//                LOGGER.error("===Failed to parse number, property setting may be wrong.", e);
//            }
//        } else {
//            LOGGER.error("===Failed to get port from property, starting server failed.");
//        }
        new ClientServer(12222).run();
    }
}

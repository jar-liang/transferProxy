package me.jar.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.util.CharsetUtil;
import io.netty.util.ReferenceCountUtil;
import me.jar.beans.HostAndPort;
import me.jar.utils.NettyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Date 2021/4/25-23:39
 */
public class ConnectRemoteHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectRemoteHandler.class);

    private Channel remoteChannel;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof FullHttpRequest) {
            FullHttpRequest httpRequest = (FullHttpRequest) msg;
            if ("/bad-request".equals(httpRequest.uri())) {
                LOGGER.error("/bad-request: " + httpRequest.toString());
            } else {
                LOGGER.info(">>>Receive Http request, uri -> {}", httpRequest.uri());
            }
            if (HttpMethod.CONNECT.equals(httpRequest.method())) {
                connectRemoteReplyClient(ctx, httpRequest);
            } else {
                sendRemote(ctx, httpRequest);
            }
        } else {
            LOGGER.warn("===Receive other date (not FullHttpRequest), skip");
            // 释放资源
            ReferenceCountUtil.release(msg);
        }
    }

    private void connectRemoteReplyClient(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        HostAndPort hostAndPort = NettyUtil.parseHostAndPort(httpRequest.uri(), 443);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(ctx.channel().eventLoop()).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("receive", new ReceiveRemoteHandler(ctx.channel()));
                    }
                });
        bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort()).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                LOGGER.debug(">>>Connecting server done (https).");
                Channel remoteChannel = future.channel();
                ChannelPipeline nearPipeline = ctx.channel().pipeline();
                ByteBuf response = Unpooled.copiedBuffer("HTTP/1.1 200 Connection Established\r\n\r\n", CharsetUtil.ISO_8859_1);
                ctx.writeAndFlush(response).addListener((ChannelFutureListener) replyFuture -> {
                    if (replyFuture.isSuccess()) {
                        LOGGER.debug("<<<Reply to client that channel has been created");
                        nearPipeline.remove("decoder");
                        nearPipeline.remove("aggregator");
                        nearPipeline.remove("connectRemote");
                        nearPipeline.addLast("justSend", new JustSendHandler(remoteChannel));
                    } else {
                        LOGGER.error("<<<Reply to client message failed!");
                    }
                });
            } else {
                LOGGER.error("<<<Connecting server failed (https)! host: " + hostAndPort.getHost() + ", port: " + hostAndPort.getPort());
                ctx.close();
            }
        });
    }

    private void sendRemote(ChannelHandlerContext ctx, FullHttpRequest httpRequest) {
        if (remoteChannel != null && remoteChannel.isActive()) {
            remoteChannel.writeAndFlush(httpRequest).addListener((ChannelFutureListener) future -> {
                if (future.isSuccess()) {
                    LOGGER.debug(">>>Sending request to server done (via exist channel).");
                } else {
                    LOGGER.error("<<<Sending request to server failed (via exist channel)!");
                }
            });
        } else {
            String address = httpRequest.headers().get(HttpHeaderNames.HOST);
            if (address == null || address.length() == 0) {
                LOGGER.error("Receive empty host address!");
                ctx.close();
                return;
            } else {
                LOGGER.info("http host address = " + address);
            }
            HostAndPort hostAndPort = NettyUtil.parseHostAndPort(address, 80);

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(ctx.channel().eventLoop()).channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline pipeline = ch.pipeline();
                            pipeline.addLast("encoder", new HttpRequestEncoder());
                            pipeline.addLast("receiveRemote", new ReceiveRemoteHandler(ctx.channel()));
                        }
                    });
            bootstrap.connect(hostAndPort.getHost(), hostAndPort.getPort()).addListener((ChannelFutureListener) connectFuture -> {
                if (connectFuture.isSuccess()) {
                    LOGGER.debug(">>>Connecting server done (http).");
                    remoteChannel = connectFuture.channel();
                    remoteChannel.writeAndFlush(httpRequest).addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            LOGGER.debug(">>>Sending request to server done.");
                        } else {
                            LOGGER.error("<<<Sending request to server failed!");
                        }
                    });
                } else {
                    LOGGER.error("<<<Connecting server failed! host: " + hostAndPort.getHost() + ", port: " + hostAndPort.getPort());
                    ctx.close();
                }
            });
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 不要打印太多日志
        LOGGER.debug("===Client channel disconnected");
        NettyUtil.closeOnFlush(remoteChannel);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===ConnectRemoteHandler caught exception", cause);
        NettyUtil.closeOnFlush(remoteChannel);
        ctx.close();
    }
}

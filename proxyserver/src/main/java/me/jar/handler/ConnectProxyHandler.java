package me.jar.handler;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.jar.constants.ProxyConstants;
import me.jar.constants.TransferMsgType;
import me.jar.exception.TransferProxyException;
import me.jar.message.TransferMsg;
import me.jar.utils.CommonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Date 2021/4/25-23:39
 */
public class ConnectProxyHandler extends CommonHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectProxyHandler.class);
    private boolean registerFlag = false;
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private Channel clientServerChannel;
    private final ChannelGroup countChannels;

    public ConnectProxyHandler(ChannelGroup countChannels) {
        this.countChannels = countChannels;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TransferMsg) {
            TransferMsg transferMsg = (TransferMsg) msg;
            if (transferMsg.getType() == TransferMsgType.REGISTER) {
                doRegister(transferMsg);
                return;
            }
            if (registerFlag) {
                switch (transferMsg.getType()) {
                    case DISCONNECT:
                        // 关闭连接
                        CHANNELS.close(channelItem -> channelItem.id().asLongText().equals(transferMsg.getMetaData().get(ProxyConstants.CHANNEL_ID)));
                        break;
                    case DATA:
                        // 传输数据
                        CHANNELS.writeAndFlush(transferMsg.getDate(), channelItem -> channelItem.id().asLongText().equals(transferMsg.getMetaData().get(ProxyConstants.CHANNEL_ID)));
                        break;
                    case KEEPALIVE:
                        // 心跳包，不处理
                        break;
                    default:
                        throw new TransferProxyException("channel is registered, message type is not one of DISCONNECT,DATA,KEEPALIVE");
                }
            } else {
                ctx.close();
            }
        }
    }

    private void doRegister(TransferMsg transferMsg) {
        TransferMsg retnTransferMsg = new TransferMsg();
        retnTransferMsg.setType(TransferMsgType.REGISTER_RESULT);
        Map<String, Object> retnMetaData = new HashMap<>();
        Map<String, Object> metaData = transferMsg.getMetaData();
        String registerKey = ProxyConstants.PROPERTY.get(ProxyConstants.REGISTER_KEY);
        if (!metaData.containsKey("password") || !registerKey.equals(metaData.get("password"))) {
            // 没有密钥或密钥错误，返回提示， 不执行注册
            retnMetaData.put("result", "0");
            retnMetaData.put("reason", "Token is wrong");
        } else {
            // 启动一个新的serverBootstrap
            EventLoopGroup bossGroup = new NioEventLoopGroup(1);
            EventLoopGroup workerGroup = new NioEventLoopGroup();
            try {
                Object portObj = metaData.get("port");
                int server2ClientPortNum = Integer.parseInt(String.valueOf(portObj));
                LOGGER.info("register port: " + server2ClientPortNum);
                ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加与客户端交互的handler
                        pipeline.addLast("byteArrayDecoder", new ByteArrayDecoder());
                        pipeline.addLast("byteArrayEncoder", new ByteArrayEncoder());
                        pipeline.addLast("connectClient", new ConnectClientHandler(channel));
                        CHANNELS.add(ch);
                    }
                };

                ServerBootstrap serverBootstrap = new ServerBootstrap();
                serverBootstrap.group(bossGroup, workerGroup).channel(NioServerSocketChannel.class)
                        .childOption(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                        .childOption(ChannelOption.SO_KEEPALIVE, true)
                        .childHandler(channelInitializer);
                ChannelFuture cf = serverBootstrap.bind(server2ClientPortNum).sync();
                clientServerChannel = cf.channel();
                countChannels.add(clientServerChannel);
                cf.channel().closeFuture().addListener((ChannelFutureListener) future -> {
                    LOGGER.error("server2Client close, bossGroup and workerGroup shutdown!");
                    bossGroup.shutdownGracefully();
                    workerGroup.shutdownGracefully();
                });
                retnMetaData.put("result", "1");
                registerFlag = true;
                LOGGER.info("server2Client starting, port is " + server2ClientPortNum);
            } catch (Exception e) {
                LOGGER.error("==server2Client starts failed, detail: " + e.getMessage());
                retnMetaData.put("result", "0");
                retnMetaData.put("reason", "client server cannot start");
                bossGroup.shutdownGracefully();
                workerGroup.shutdownGracefully();
            }
        }
        retnTransferMsg.setMetaData(retnMetaData);
        channel.writeAndFlush(retnTransferMsg);
        if (!registerFlag) {
            LOGGER.error("client agent registered failed, reason: " + retnMetaData.get("reason"));
            channel.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.warn("===server agent to client agent connection inactive. channel: " + ctx.channel().toString());
        if (clientServerChannel != null) {
            LOGGER.warn("due to server agent and client agent connection inactive, server2Client has to close. channel: " + clientServerChannel.toString());
            clientServerChannel.close();
        }
    }
}

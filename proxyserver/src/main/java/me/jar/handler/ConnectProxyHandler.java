package me.jar.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.jar.constants.ProxyConstants;
import me.jar.constants.TransferMsgType;
import me.jar.exception.TransferProxyException;
import me.jar.message.TransferMsg;
import me.jar.utils.CommonHandler;
import me.jar.utils.NettyUtil;
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
//                if (!metaData.containsKey("password") || "1qaz!QAZ".equals(metaData.get("password"))) {
        if (false) { // todo 测试先不管密钥
            // 没有密钥或密钥错误，返回提示， 不执行注册
            retnMetaData.put("result", "0");
            retnMetaData.put("reason", "Token is wrong");
            retnTransferMsg.setMetaData(retnMetaData);
            channel.writeAndFlush(retnTransferMsg);
        } else {
            // 启动一个新的serverBootstrap
            int port = 9999; // 暂定
            try {
                ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        // 添加与客户端交互的handler
                        pipeline.addLast("byteArrayDecoder", new ByteArrayDecoder());
                        pipeline.addLast("byteArrayEncoder", new ByteArrayEncoder());
                        pipeline.addLast("connectClient", new ConnectClientHandler(channel));
                        CHANNELS.add(ch);
                        clientServerChannel = ch;
                    }
                };
                NettyUtil.starServer(port, channelInitializer);
                metaData.put("result", "1");
                registerFlag = true;
                LOGGER.info("client server starting, port is " + port);
            } catch (Exception e) {
                LOGGER.error("==client server starts failed, detail: " + e.getMessage());
                metaData.put("result", "0");
                metaData.put("reason", "client server cannot start");
            }
        }
        retnTransferMsg.setMetaData(metaData);
        channel.writeAndFlush(retnTransferMsg);
        if (!registerFlag) {
            LOGGER.error("client agent registered failed, reason: " + retnMetaData.get("reason"));
            channel.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 不要打印太多日志
        LOGGER.info("===ConnectProxyHandler执行channelInactive");
        if (clientServerChannel != null) {
            clientServerChannel.close();
        }
    }
}

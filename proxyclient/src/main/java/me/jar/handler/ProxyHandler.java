package me.jar.handler;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.bytes.ByteArrayDecoder;
import io.netty.handler.codec.bytes.ByteArrayEncoder;
import io.netty.util.ReferenceCountUtil;
import me.jar.constants.ProxyConstants;
import me.jar.constants.TransferMsgType;
import me.jar.exception.TransferProxyException;
import me.jar.message.TransferMsg;
import me.jar.utils.CommonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Description
 * @Date 2021/4/27-21:50
 */
public class ProxyHandler extends CommonHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class);
//    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final Map<String, Channel> CHANNEL_MAP = new ConcurrentHashMap<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof TransferMsg) {
            TransferMsg transferMsg = (TransferMsg) msg;
            TransferMsgType type = transferMsg.getType();
            Map<String, Object> metaData = transferMsg.getMetaData();
            String channelId = String.valueOf(metaData.get(ProxyConstants.CHANNEL_ID));
            switch (type) {
                case REGISTER_RESULT:
                    if ("1".equals(metaData.get("result"))) {
                        LOGGER.info("register to transfer proxy server successfully");
                    } else {
                        LOGGER.error("register failed, reason: " + metaData.get("reason"));
                        ctx.close();
                    }
                    break;
                case CONNECT:
                    connectTarget(ctx, msg, metaData);
                    break;
                case DISCONNECT:
                    Channel channel = CHANNEL_MAP.get(channelId);
                    if (channel != null) {
                        channel.close();
                        CHANNEL_MAP.remove(channelId);
                    }
                    break;
                case DATA:
                    Channel channelData = CHANNEL_MAP.get(channelId);
                    if (channelData != null) {
                        channelData.writeAndFlush(transferMsg.getDate());
                    }
                    break;
                case KEEPALIVE:
                    break;
                default:
                    throw new TransferProxyException("unknown type: " + type.getType());
            }
        }
    }

    private void connectTarget(ChannelHandlerContext ctx, Object msg, Map<String, Object> metaData) {
        String channelId = String.valueOf(metaData.get(ProxyConstants.CHANNEL_ID));
        EventLoopGroup workGroup = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(workGroup).channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000).handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                pipeline.addLast("byteArrayDecoder", new ByteArrayDecoder());
                pipeline.addLast("byteArrayEncoder", new ByteArrayEncoder());
                pipeline.addLast("clientHandler", new ClientHandler(ctx.channel(), channelId));
                CHANNEL_MAP.put(channelId, ch);
            }
        });
        String host = "482251u81s.qicp.vip";
        int port = 54573;
        bootstrap.connect(host, port).addListener((ChannelFutureListener) connectFuture -> {
            if (connectFuture.isSuccess()) {
                LOGGER.debug(">>>Connect target server successfully.");
            } else {
                LOGGER.error("===Failed to connect to target server! host: " + host + " , port: " + port);
                ReferenceCountUtil.release(msg);
                TransferMsg disconnectMsg = new TransferMsg();
                disconnectMsg.setType(TransferMsgType.DISCONNECT);
                Map<String, Object> failMetaData = new HashMap<>(1);
                failMetaData.put(ProxyConstants.CHANNEL_ID, channelId);
                disconnectMsg.setMetaData(failMetaData);
                ctx.writeAndFlush(disconnectMsg);
                CHANNEL_MAP.remove(channelId);
            }
        });
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(TransferMsgType.REGISTER);
        Map<String, Object> metaData = new HashMap<>(1);
        metaData.put("password", "1qaz!QAZ");
        transferMsg.setMetaData(metaData);
        ctx.writeAndFlush(transferMsg);
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("===proxy server connection failed, please check!.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===proxy channel has caught exception, cause: {}", cause.getMessage());
        ctx.close();
    }
}

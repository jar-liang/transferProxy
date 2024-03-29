package me.jar.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import me.jar.constants.ProxyConstants;
import me.jar.constants.TransferMsgType;
import me.jar.message.TransferMsg;
import me.jar.utils.NettyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;


/**
 * @Description
 * @Date 2021/4/25-23:39
 */
public class ConnectClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectClientHandler.class);
    private Channel proxyServer;
    private ChannelGroup channels;
    private int lastLength = 0;
    private String proxyType = ProxyConstants.PROPERTY.get(ProxyConstants.PROXY_TYPE);

    public ConnectClientHandler(Channel proxyServer, ChannelGroup channels) {
        this.proxyServer = proxyServer;
        this.channels = channels;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
//            if (ProxyConstants.TYPE_HTTP.equals(proxyType)) {
//                if (lastLength > 10240) {
//                    Thread.sleep(100L);
//                    if (!channelMap.containsKey(channelId)) {
//                        LOGGER.warn("channelMap has no channel, its id: " + channelId + ", stop sending data!");
//                        return;
//                    }
//                }
//                lastLength = bytes.length;
//            }
            TransferMsg transferMsg = new TransferMsg();
            transferMsg.setType(TransferMsgType.DATA);
            Map<String, Object> metaData = new HashMap<>(1);
            metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
            transferMsg.setMetaData(metaData);
            transferMsg.setDate(bytes);
            proxyServer.writeAndFlush(transferMsg);
        }
    }

//    @Override // 不需要，因为客户端那边会根据是否有连接进行自动连接，不需根据CONNECT消息进行
//    public void channelActive(ChannelHandlerContext ctx) throws Exception {
//        TransferMsg transferMsg = new TransferMsg();
//        transferMsg.setType(TransferMsgType.CONNECT);
//        Map<String, Object> metaData = new HashMap<>(1);
//        metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
//        transferMsg.setMetaData(metaData);
//        proxyServer.writeAndFlush(transferMsg);
//    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(TransferMsgType.DISCONNECT);
        Map<String, Object> metaData = new HashMap<>(1);
        metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
        transferMsg.setMetaData(metaData);
        proxyServer.writeAndFlush(transferMsg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===server2Client caught exception. channel:" + ctx.channel().toString() + ". cause: " + cause.getMessage());
        ctx.close();
    }

    /**
     * 判断是否断开连接，当写空闲时间超过10s，关闭连接，并发送连接断开消息给client agent
     * @param ctx
     * @param evt
     */
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.WRITER_IDLE) {
                LOGGER.warn("no data from server more than 10s, close connection");
                channels.close(channel -> ctx.channel().id().compareTo(channel.id()) == 0);
                // 发送DISCONNECT消息 close会调用channelInactive
                NettyUtil.closeOnFlush(ctx.channel());
            }
        }
    }
}

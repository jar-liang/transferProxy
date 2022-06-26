package me.jar.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.jar.constants.ProxyConstants;
import me.jar.constants.TransferMsgType;
import me.jar.message.TransferMsg;
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
    private boolean isReady = false;

    public ConnectClientHandler(Channel proxyServer) {
        this.proxyServer = proxyServer;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        if (isReady) {
            if (msg instanceof byte[]) {
                byte[] bytes = (byte[]) msg;
                TransferMsg transferMsg = new TransferMsg();
                transferMsg.setType(TransferMsgType.DATA);
                Map<String, Object> metaData = new HashMap<>(1);
                metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
                transferMsg.setMetaData(metaData);
                transferMsg.setDate(bytes);
                proxyServer.writeAndFlush(transferMsg);
            }
        } else {
            Thread.sleep(5000L);
            isReady = true;
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        TransferMsg transferMsg = new TransferMsg();
        transferMsg.setType(TransferMsgType.CONNECT);
        Map<String, Object> metaData = new HashMap<>(1);
        metaData.put(ProxyConstants.CHANNEL_ID, ctx.channel().id().asLongText());
        transferMsg.setMetaData(metaData);
        proxyServer.writeAndFlush(transferMsg);
    }

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
        LOGGER.error("===ConnectRemoteHandler caught exception", cause);
        ctx.close();
    }
}

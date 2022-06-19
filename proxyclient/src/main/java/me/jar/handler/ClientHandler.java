package me.jar.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Date 2021/4/27-21:50
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private Channel proxyChannel;

    public ClientHandler(Channel proxyChannel) {
        this.proxyChannel = proxyChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (proxyChannel != null && proxyChannel.isActive()) {
            proxyChannel.writeAndFlush(msg);
        } else {
            LOGGER.info("===proxy channel is null or not active, close remote rdp channel");
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("===remote rdp channel inactive.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===remote rdp channel has caught exception, cause: {}", cause.getMessage());
        ctx.close();
    }
}

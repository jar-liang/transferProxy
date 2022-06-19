package me.jar.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import me.jar.channel.ChannelDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @Description
 * @Date 2021/4/25-23:39
 */
public class ConnectClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectClientHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (ChannelDTO.clientChannel == null || !ChannelDTO.clientChannel.isActive()) {
            ChannelDTO.clientChannel = ctx.channel();
        }
        if (ChannelDTO.proxyChannel != null && ChannelDTO.proxyChannel.isActive()) {
            ChannelDTO.proxyChannel.writeAndFlush(msg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 不要打印太多日志
        LOGGER.info("===ConnectClientHandler执行channelInactive");
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===ConnectRemoteHandler caught exception", cause);
        ctx.close();
    }
}

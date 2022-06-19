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
public class ConnectProxyHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectProxyHandler.class);
    private static final ReentrantLock LOCK = new ReentrantLock();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        LOCK.lock();
        try {
            if (ChannelDTO.proxyChannel == null || !ChannelDTO.proxyChannel.isActive()) {
                ChannelDTO.proxyChannel = ctx.channel();
            }
        } finally {
            LOCK.unlock();
        }

        if (ChannelDTO.clientChannel != null && ChannelDTO.clientChannel.isActive()) {
            ChannelDTO.clientChannel.writeAndFlush(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        LOGGER.info("proxy server channel active...");
        LOCK.lock();
        try {
            if (ChannelDTO.proxyChannel == null || !ChannelDTO.proxyChannel.isActive()) {
                ChannelDTO.proxyChannel = ctx.channel();
            }
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 不要打印太多日志
        LOGGER.info("===ConnectProxyHandler执行channelInactive");
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===ConnectProxyHandler caught exception", cause);
        ctx.close();
    }
}

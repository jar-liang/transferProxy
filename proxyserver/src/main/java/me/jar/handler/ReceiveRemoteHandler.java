package me.jar.handler;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import me.jar.utils.NettyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Date 2021/4/26-0:07
 */
public class ReceiveRemoteHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReceiveRemoteHandler.class);

    private final Channel nearChannel;

    public ReceiveRemoteHandler(Channel nearChannel) {
        this.nearChannel = nearChannel;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        if (nearChannel.isActive()) {
            nearChannel.writeAndFlush(msg);
        } else {
            LOGGER.info("===Client channel disconnected, no transferring data.");
            ReferenceCountUtil.release(msg);
            ctx.close();
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.debug("===Remote channel disconnected");
        NettyUtil.closeOnFlush(nearChannel);
        ctx.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===ReceiveRemoteHandler caught exception, cause: {}", cause.getMessage() + ". host: " + ctx.channel().remoteAddress().toString());
        NettyUtil.closeOnFlush(nearChannel);
        ctx.close();
    }
}

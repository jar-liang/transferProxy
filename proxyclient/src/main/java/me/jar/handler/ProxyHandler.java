package me.jar.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.jar.constants.TransferMsgType;
import me.jar.message.TransferMsg;
import me.jar.utils.CommonHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Date 2021/4/27-21:50
 */
public class ProxyHandler extends CommonHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProxyHandler.class);
    private static final ChannelGroup CHANNELS = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
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
        LOGGER.info("===proxy channel is inactive.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===proxy channel has caught exception, cause: {}", cause.getMessage());
        ctx.close();
    }
}

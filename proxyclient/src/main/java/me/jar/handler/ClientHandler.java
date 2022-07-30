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
 * @Date 2021/4/27-21:50
 */
public class ClientHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);
    private Channel proxyChannel;
    private String channelId;
    private final Map<String, Channel> channelMap;
    private int lastLength = 0;
    private boolean isNeedWaiting = ProxyConstants.TYPE_HTTP.equals(ProxyConstants.PROPERTY.get(ProxyConstants.PROXY_TYPE));

    public ClientHandler(Channel proxyChannel, String channelId, Map<String, Channel> channelMap) {
        this.proxyChannel = proxyChannel;
        this.channelId = channelId;
        this.channelMap = channelMap;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws InterruptedException {
        if (msg instanceof byte[]) {
            byte[] bytes = (byte[]) msg;
            if (isNeedWaiting) {
                if (lastLength > 10240) {
                    Thread.sleep(100L);
                    if (!channelMap.containsKey(channelId)) {
                        LOGGER.warn("channelMap has no channel, its id: " + channelId + ", stop sending data!");
                        return;
                    }
                }
                lastLength = bytes.length;
            }
            Map<String, Object> metaData = new HashMap<>(1);
            metaData.put(ProxyConstants.CHANNEL_ID, channelId);
            TransferMsg transferMsg = new TransferMsg();
            transferMsg.setType(TransferMsgType.DATA);
            transferMsg.setMetaData(metaData);
            transferMsg.setDate(bytes);
            proxyChannel.writeAndFlush(transferMsg);
        }
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        LOGGER.info("===target channel inactive. channel: " + ctx.channel().toString());
        closeChannelAndMapRemove(channelId);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        LOGGER.error("===target channel has caught exception, cause: {}", cause.getMessage());
        ctx.close();
    }

    private void closeChannelAndMapRemove(String channelId) {
        Channel channel = channelMap.get(channelId);
        if (channel != null) {
            channelMap.remove(channelId);
        }
    }
}

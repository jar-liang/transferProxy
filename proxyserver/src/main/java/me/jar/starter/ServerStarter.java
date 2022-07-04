package me.jar.starter;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import me.jar.constants.ProxyConstants;
import me.jar.handler.ConnectProxyHandler;
import me.jar.utils.Byte2TransferMsgDecoder;
import me.jar.utils.LengthContentDecoder;
import me.jar.utils.NettyUtil;
import me.jar.utils.TransferMsg2ByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @Description
 * @Date 2021/4/23-23:45
 */
public class ServerStarter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerStarter.class);

    public void runForProxy(int port) {
        ChannelInitializer<SocketChannel> channelInitializer = new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
                ChannelPipeline pipeline = ch.pipeline();
                // 添加与客户端交互的handler
                pipeline.addLast("lengthContent", new LengthContentDecoder());
                pipeline.addLast("decoder", new Byte2TransferMsgDecoder());
                pipeline.addLast("encoder", new TransferMsg2ByteEncoder());
                pipeline.addLast("idleEvt", new IdleStateHandler(60, 30, 0));
                pipeline.addLast("connectProxy", new ConnectProxyHandler());
            }
        };
        NettyUtil.starServer(port, channelInitializer);
    }

    public static void main(String[] args) {
        if (ProxyConstants.PROPERTY.containsKey(ProxyConstants.SERVER_LISTEN_PORT)) {
            String port = ProxyConstants.PROPERTY.get(ProxyConstants.SERVER_LISTEN_PORT);
            try {
                int portNum = Integer.parseInt(port.trim());
                new ServerStarter().runForProxy(portNum);
            } catch (NumberFormatException e) {
                LOGGER.error("===Failed to parse number, property setting may be wrong.", e);
            }
        } else {
            LOGGER.error("===Failed to get port from property, starting server failed.");
        }

    }
}

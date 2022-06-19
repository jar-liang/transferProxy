package me.jar.channel;

import io.netty.channel.Channel;

public class ChannelDTO {
    public static Channel proxyChannel = null;
    public static Channel clientChannel = null;
    public static final Object LOCK = new Object();
}

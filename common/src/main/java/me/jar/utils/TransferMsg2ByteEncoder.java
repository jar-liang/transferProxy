package me.jar.utils;

import com.alibaba.fastjson.JSON;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import me.jar.message.TransferMsg;

import java.util.Map;

public class TransferMsg2ByteEncoder extends MessageToByteEncoder<TransferMsg> {
    @Override
    protected void encode(ChannelHandlerContext ctx, TransferMsg msg, ByteBuf out) throws Exception {
        int type = msg.getType().getType();
        Map<String, Object> metaData = msg.getMetaData();
        byte[] metaDataBytes = JSON.toJSONBytes(metaData);
        ByteBuf byteBuf = Unpooled.copyInt(type, metaDataBytes.length);

        if (msg.getDate() != null && msg.getDate().length > 0) {
            ByteBuf typeAndMetaDataAndData = Unpooled.wrappedBuffer(byteBuf, Unpooled.wrappedBuffer(metaDataBytes), Unpooled.wrappedBuffer(msg.getDate()));
            out.writeInt(typeAndMetaDataAndData.readableBytes());
            out.writeBytes(typeAndMetaDataAndData);
        } else {
            ByteBuf typeAndMetaData = Unpooled.wrappedBuffer(byteBuf, Unpooled.wrappedBuffer(metaDataBytes));
            out.writeInt(typeAndMetaData.readableBytes());
            out.writeBytes(typeAndMetaData);
        }
    }
}

package me.lain.muxtun.codec;

import java.util.List;
import java.util.UUID;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;
import me.lain.muxtun.codec.Message.MessageType;

public class MessageDecoder extends ByteToMessageDecoder
{

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        switch (MessageType.find(in.readByte()))
        {
            case Ping:
                out.add(new Message()
                        .setType(MessageType.Ping));
                break;
            case Open:
                out.add(new Message()
                        .setType(MessageType.Open)
                        .setStreamId(new UUID(in.readLong(), in.readLong())));
                break;
            case Data:
                out.add(new Message()
                        .setType(MessageType.Data)
                        .setStreamId(new UUID(in.readLong(), in.readLong()))
                        .setPayload(in.readRetainedSlice(in.readableBytes())));
                break;
            case Drop:
                out.add(new Message()
                        .setType(MessageType.Drop)
                        .setStreamId(new UUID(in.readLong(), in.readLong())));
                break;
            case OpenUDP:
                out.add(new Message()
                        .setType(MessageType.OpenUDP)
                        .setStreamId(new UUID(in.readLong(), in.readLong())));
                break;
            default:
                throw new CorruptedFrameException("UnknownMessageType");
        }
    }

}

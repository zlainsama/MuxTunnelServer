package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.codec.Message.MessageType;

public class MessageEncoder extends MessageToByteEncoder<Message>
{

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception
    {
        try
        {
            switch (msg.getType())
            {
                case Ping:
                    out.writeByte(MessageType.Ping.getId());
                    break;
                case Open:
                    out.writeByte(MessageType.Open.getId());
                    out.writeLong(msg.getStreamId().getMostSignificantBits()).writeLong(msg.getStreamId().getLeastSignificantBits());
                    break;
                case Data:
                    out.writeByte(MessageType.Data.getId());
                    out.writeLong(msg.getStreamId().getMostSignificantBits()).writeLong(msg.getStreamId().getLeastSignificantBits());
                    out.writeBytes(msg.getPayload());
                    break;
                case Drop:
                    out.writeByte(MessageType.Drop.getId());
                    out.writeLong(msg.getStreamId().getMostSignificantBits()).writeLong(msg.getStreamId().getLeastSignificantBits());
                    break;
                default:
                    throw new IllegalArgumentException("UnknownMessageType");
            }
        }
        finally
        {
            ReferenceCountUtil.release(msg.getPayload());
        }
    }

}

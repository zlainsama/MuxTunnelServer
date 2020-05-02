package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MessageEncoder extends MessageToByteEncoder<Message>
{

    @Override
    protected ByteBuf allocateBuffer(ChannelHandlerContext ctx, Message msg, boolean preferDirect) throws Exception
    {
        if (preferDirect)
            return ctx.alloc().ioBuffer(4 + msg.size());
        else
            return ctx.alloc().heapBuffer(4 + msg.size());
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, Message msg, ByteBuf out) throws Exception
    {
        msg.encode(out.writeMedium(1 + msg.size()).writeByte(msg.type().getId()));
    }

}

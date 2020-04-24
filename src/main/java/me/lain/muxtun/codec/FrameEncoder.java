package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class FrameEncoder extends MessageToByteEncoder<ByteBuf>
{

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) throws Exception
    {
        int i = msg.readableBytes();
        int j = Vars.computeVarIntSize(i);

        if (j > 3)
            throw new IllegalArgumentException("MessageTooBig " + i);

        out.ensureWritable(j + i);
        Vars.writeVarInt(out, i).writeBytes(msg, msg.readerIndex(), i);
    }

}

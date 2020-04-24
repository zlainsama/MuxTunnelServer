package me.lain.muxtun.codec;

import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.handler.codec.CorruptedFrameException;

public class FrameDecoder extends ByteToMessageDecoder
{

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception
    {
        in.markReaderIndex();
        byte[] buffer = new byte[3];

        for (int i = 0; i < buffer.length; ++i)
        {
            if (!in.isReadable())
            {
                in.resetReaderIndex();
                return;
            }

            buffer[i] = in.readByte();
            if (buffer[i] >= 0)
            {
                ByteBuf buf = Unpooled.wrappedBuffer(buffer);

                try
                {
                    int j = Vars.readVarInt(buf);
                    if (in.readableBytes() >= j)
                    {
                        out.add(in.readRetainedSlice(j));
                        return;
                    }
                    else
                    {
                        in.resetReaderIndex();
                        return;
                    }
                }
                finally
                {
                    buf.release();
                }
            }
        }

        throw new CorruptedFrameException("MessageTooBig");
    }

}

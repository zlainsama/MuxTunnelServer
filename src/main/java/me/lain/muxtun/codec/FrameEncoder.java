package me.lain.muxtun.codec;

import java.util.List;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.CompositeByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldPrepender;

public class FrameEncoder extends LengthFieldPrepender
{

    public FrameEncoder()
    {
        super(3);
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception
    {
        super.encode(ctx, msg, out);

        if (out.size() > 1)
        {
            CompositeByteBuf buf = ctx.alloc().compositeBuffer(out.size());
            for (int i = 0; i < out.size(); i++)
                buf.addComponent(true, (ByteBuf) out.get(i));
            out.clear();
            out.add(buf);
        }
    }

}

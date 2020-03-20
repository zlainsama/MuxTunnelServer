package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

@ChannelHandler.Sharable
public class MessageEncoder extends ChannelOutboundHandlerAdapter
{

    public static final MessageEncoder DEFAULT = new MessageEncoder();

    protected Object encode(ChannelHandlerContext ctx, Message msg) throws Exception
    {
        boolean release = true;
        ByteBuf result = null;
        try
        {
            result = ctx.alloc().buffer().writeByte(msg.type().getId());
            msg.encode(result);
            release = false;
            return result;
        }
        finally
        {
            if (release)
                ReferenceCountUtil.release(result);
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
        if (msg instanceof Message)
        {
            Message cast = (Message) msg;

            try
            {
                Object result = encode(ctx, cast);
                if (result != null)
                    ctx.write(result, promise);
                else
                    throw new Error("BadEncoder");
            }
            finally
            {
                ReferenceCountUtil.release(cast);
            }
        }
        else
        {
            ctx.write(msg, promise);
        }
    }

}

package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.codec.Message.MessageType;

@ChannelHandler.Sharable
public class MessageDecoder extends ChannelInboundHandlerAdapter
{

    public static final MessageDecoder DEFAULT = new MessageDecoder();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof ByteBuf)
        {
            ByteBuf cast = (ByteBuf) msg;

            try
            {
                Object result = decode(ctx, cast);
                if (result != null)
                    ctx.fireChannelRead(result);
                else
                    throw new Error("BadDecoder");
            }
            finally
            {
                ReferenceCountUtil.release(cast);
            }
        }
        else
        {
            ctx.fireChannelRead(msg);
        }
    }

    protected Object decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception
    {
        boolean release = true;
        Message result = null;
        try
        {
            result = MessageType.find(msg.readByte()).create();
            result.decode(msg);
            release = false;
            return result;
        }
        finally
        {
            if (release)
                ReferenceCountUtil.release(result);
        }
    }

}

package me.lain.muxtun.mipo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

@Sharable
class TCPStreamInboundHandler extends ChannelInboundHandlerAdapter
{

    static final TCPStreamInboundHandler DEFAULT = new TCPStreamInboundHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof ByteBuf)
        {
            ByteBuf cast = (ByteBuf) msg;

            try
            {
                handleMessage(ctx, cast);
            }
            finally
            {
                ReferenceCountUtil.release(cast);
            }
        }
        else
        {
            try
            {
                ctx.close();
            }
            finally
            {
                ReferenceCountUtil.release(msg);
            }
        }
    }

    private void handleMessage(ChannelHandlerContext ctx, ByteBuf msg) throws Exception
    {
        if (!ctx.channel().attr(Vars.WRITER_KEY).get().write(msg.retain()))
            ctx.close();
    }

}

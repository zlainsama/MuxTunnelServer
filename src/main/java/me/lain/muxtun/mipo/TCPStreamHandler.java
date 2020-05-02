package me.lain.muxtun.mipo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;

@Sharable
class TCPStreamHandler extends ChannelInboundHandlerAdapter
{

    static final TCPStreamHandler DEFAULT = new TCPStreamHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof ByteBuf)
        {
            ByteBuf cast = (ByteBuf) msg;

            try
            {
                handleMessage(StreamContext.getContext(ctx.channel()), cast);
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

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        Vars.ChannelError.accumulate(ctx.channel(), cause);

        ctx.close();
    }

    private void handleMessage(StreamContext sctx, ByteBuf msg) throws Exception
    {
        if (sctx.isActive() && !sctx.getPayloadWriter().writeSlices(msg.retain()))
            sctx.close();
    }

}

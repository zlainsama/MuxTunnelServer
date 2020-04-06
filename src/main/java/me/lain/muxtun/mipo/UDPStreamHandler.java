package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

@Sharable
class UDPStreamHandler extends ChannelInboundHandlerAdapter
{

    static final UDPStreamHandler DEFAULT = new UDPStreamHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof DatagramPacket)
        {
            DatagramPacket cast = (DatagramPacket) msg;

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

    private void handleMessage(StreamContext sctx, DatagramPacket msg) throws Exception
    {
        if (sctx.isActive() && !sctx.getPayloadWriter().writeSlices(msg.content().retain(), 65536, null))
            sctx.close();
    }

}

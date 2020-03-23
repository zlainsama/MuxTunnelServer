package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;

@Sharable
class UDPStreamInboundHandler extends ChannelInboundHandlerAdapter
{

    static final UDPStreamInboundHandler DEFAULT = new UDPStreamInboundHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof DatagramPacket)
        {
            DatagramPacket cast = (DatagramPacket) msg;

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

    private void handleMessage(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception
    {
        if (!ctx.channel().attr(Vars.WRITER_KEY).get().writeSlices(msg.content().retain(), 65536, null))
            ctx.close();
    }

}

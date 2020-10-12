package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.Shared;
import me.lain.muxtun.util.SimpleLogger;

@Sharable
class UdpStreamHandler extends ChannelInboundHandlerAdapter {

    static final UdpStreamHandler DEFAULT = new UdpStreamHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof DatagramPacket) {
            DatagramPacket cast = (DatagramPacket) msg;

            try {
                handleMessage(StreamContext.getContext(ctx.channel()), cast);
            } finally {
                ReferenceCountUtil.release(cast);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        ctx.close().addListener(future -> SimpleLogger.println("%s > udp stream connection %s closed with unexpected error. (%s)", Shared.printNow(), ctx.channel().id(), cause));
    }

    private void handleMessage(StreamContext sctx, DatagramPacket msg) throws Exception {
        if (sctx.isActive() && !sctx.getPayloadWriter().writeSlices(msg.content().retain()))
            sctx.close();
    }

}

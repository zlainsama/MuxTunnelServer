package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.socket.DatagramPacket;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
class UdpStreamHandler extends ChannelInboundHandlerAdapter {

    static final UdpStreamHandler DEFAULT = new UdpStreamHandler();
    private static final Logger logger = LoggerFactory.getLogger(UdpStreamHandler.class);

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
        ctx.close().addListener(future -> logger.error("closed udp stream connection due to error", cause));
    }

    private void handleMessage(StreamContext sctx, DatagramPacket msg) throws Exception {
        if (sctx.isActive() && !sctx.getPayloadWriter().writeSlices(msg.content().retain()))
            sctx.close();
    }

}

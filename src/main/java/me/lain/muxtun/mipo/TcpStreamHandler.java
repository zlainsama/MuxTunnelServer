package me.lain.muxtun.mipo;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
class TcpStreamHandler extends ChannelInboundHandlerAdapter {

    static final TcpStreamHandler DEFAULT = new TcpStreamHandler();
    private static final Logger logger = LoggerFactory.getLogger(TcpStreamHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf cast = (ByteBuf) msg;

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
        ctx.close().addListener(future -> logger.error("closed tcp stream connection due to error", cause));
    }

    private void handleMessage(StreamContext sctx, ByteBuf msg) throws Exception {
        if (sctx.isActive() && !sctx.getPayloadWriter().writeSlices(msg.retain()))
            sctx.close();
    }

}

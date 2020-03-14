package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
class TCPStreamExceptionHandler extends ChannelInboundHandlerAdapter
{

    static final TCPStreamExceptionHandler DEFAULT = new TCPStreamExceptionHandler();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (ctx.channel().attr(Vars.ERROR_KEY).get() != null || !ctx.channel().attr(Vars.ERROR_KEY).compareAndSet(null, cause))
            ctx.channel().attr(Vars.ERROR_KEY).get().addSuppressed(cause);

        ctx.close();
    }

}

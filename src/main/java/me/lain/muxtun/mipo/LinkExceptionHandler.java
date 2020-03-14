package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
class LinkExceptionHandler extends ChannelInboundHandlerAdapter
{

    static final LinkExceptionHandler DEFAULT = new LinkExceptionHandler();

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (ctx.channel().attr(Vars.ERROR_KEY).get() != null || !ctx.channel().attr(Vars.ERROR_KEY).compareAndSet(null, cause))
            ctx.channel().attr(Vars.ERROR_KEY).get().addSuppressed(cause);

        ctx.close();
    }

}

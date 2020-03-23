package me.lain.muxtun.mipo;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
class LinkWritabilityChangeListener extends ChannelInboundHandlerAdapter
{

    private static final Consumer<StreamContext> DISABLEAUTOREAD = sctx -> sctx.getChannel().config().setAutoRead(false);
    private static final Consumer<StreamContext> UPDATEWINDOWSIZEWITHZEROINCREMENT = sctx -> sctx.updateWindowSize(IntUnaryOperator.identity());

    static final LinkWritabilityChangeListener DEFAULT = new LinkWritabilityChangeListener();

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
    {
        if (ctx.channel().isWritable())
            ctx.channel().attr(Vars.SESSION_KEY).get().ongoingStreams.values().forEach(UPDATEWINDOWSIZEWITHZEROINCREMENT);
        else
            ctx.channel().attr(Vars.SESSION_KEY).get().ongoingStreams.values().forEach(DISABLEAUTOREAD);

        ctx.fireChannelWritabilityChanged();
    }

}

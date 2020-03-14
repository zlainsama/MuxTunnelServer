package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

@Sharable
class LinkWritabilityChangeListener extends ChannelInboundHandlerAdapter
{

    static final LinkWritabilityChangeListener DEFAULT = new LinkWritabilityChangeListener();

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
    {
        boolean isWritable = ctx.channel().isWritable();
        ctx.channel().attr(Vars.SESSION_KEY).get().ongoingStreams.values().forEach(s -> s.config().setAutoRead(isWritable));

        ctx.fireChannelWritabilityChanged();
    }

}

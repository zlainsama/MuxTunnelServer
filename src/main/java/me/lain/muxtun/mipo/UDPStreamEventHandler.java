package me.lain.muxtun.mipo;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;

@Sharable
class UDPStreamEventHandler extends ChannelInboundHandlerAdapter
{

    static final UDPStreamEventHandler DEFAULT = new UDPStreamEventHandler();

    private void handleEvent(ChannelHandlerContext ctx, IdleStateEvent evt) throws Exception
    {
        if (evt.state() == IdleState.ALL_IDLE)
            ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
    {
        if (evt instanceof IdleStateEvent)
        {
            handleEvent(ctx, (IdleStateEvent) evt);
        }
        else
        {
            ctx.fireUserEventTriggered(evt);
        }
    }

}

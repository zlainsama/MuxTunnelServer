package me.lain.muxtun.mipo;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

class StreamContext
{

    static StreamContext getContext(Channel channel)
    {
        return channel.attr(Vars.STREAMCONTEXT_KEY).get();
    }

    private final UUID streamId;
    private final LinkSession session;
    private final Channel channel;
    private final AtomicInteger quota;
    private final PayloadWriter payloadWriter;

    StreamContext(UUID streamId, LinkSession session, Channel channel)
    {
        this.streamId = streamId;
        this.session = session;
        this.channel = channel;
        this.quota = new AtomicInteger(2097152);
        this.payloadWriter = session.newPayloadWriter(this);
    }

    ChannelFuture close()
    {
        return getChannel().close();
    }

    Channel getChannel()
    {
        return channel;
    }

    PayloadWriter getPayloadWriter()
    {
        return payloadWriter;
    }

    LinkSession getSession()
    {
        return session;
    }

    UUID getStreamId()
    {
        return streamId;
    }

    boolean isActive()
    {
        return getChannel().isActive();
    }

    int updateQuota(IntUnaryOperator updateFunction)
    {
        int num = quota.updateAndGet(updateFunction);
        getChannel().config().setAutoRead(isActive() && getSession().isActive() && getSession().getFlowControl().window() > 0 && num > 0);
        return num;
    }

    ChannelFuture writeAndFlush(Object msg)
    {
        return getChannel().writeAndFlush(msg);
    }

}

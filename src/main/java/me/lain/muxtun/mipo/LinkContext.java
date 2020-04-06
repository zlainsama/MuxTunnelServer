package me.lain.muxtun.mipo;

import java.util.Comparator;
import java.util.concurrent.atomic.AtomicInteger;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import me.lain.muxtun.util.RoundTripTimeMeasurement;
import me.lain.muxtun.util.SmoothedRoundTripTime;

class LinkContext
{

    static final Comparator<Channel> SORTER = Comparator.comparingLong(channel -> {
        LinkContext context = LinkContext.getContext(channel);
        return context.getSRTT().get() * (1 + context.getCount().get());
    });

    static LinkContext getContext(Channel channel)
    {
        return channel.attr(Vars.LINKCONTEXT_KEY).get();
    }

    private final LinkManager manager;
    private final Channel channel;
    private final RoundTripTimeMeasurement RTTM;
    private final SmoothedRoundTripTime SRTT;
    private final AtomicInteger count;
    private volatile LinkSession session;

    LinkContext(LinkManager manager, Channel channel)
    {
        this.manager = manager;
        this.channel = channel;
        this.RTTM = new RoundTripTimeMeasurement();
        this.SRTT = new SmoothedRoundTripTime();
        this.count = new AtomicInteger();
    }

    ChannelFuture close()
    {
        return getChannel().close();
    }

    Channel getChannel()
    {
        return channel;
    }

    AtomicInteger getCount()
    {
        return count;
    }

    LinkManager getManager()
    {
        return manager;
    }

    RoundTripTimeMeasurement getRTTM()
    {
        return RTTM;
    }

    LinkSession getSession()
    {
        return session;
    }

    SmoothedRoundTripTime getSRTT()
    {
        return SRTT;
    }

    boolean isActive()
    {
        return getChannel().isActive();
    }

    LinkContext setSession(LinkSession session)
    {
        this.session = session;
        return this;
    }

    ChannelFuture writeAndFlush(Object msg)
    {
        return getChannel().writeAndFlush(msg);
    }

}

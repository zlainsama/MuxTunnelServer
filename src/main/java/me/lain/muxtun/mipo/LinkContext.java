package me.lain.muxtun.mipo;

import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.concurrent.EventExecutor;
import me.lain.muxtun.util.RoundTripTimeMeasurement;
import me.lain.muxtun.util.SmoothedRoundTripTime;

class LinkContext
{

    static final Comparator<Channel> SORTER = Comparator.comparingLong(channel -> {
        LinkContext context = LinkContext.getContext(channel);
        return context.getSRTT().get() + context.getSRTT().var() * (1 + 2 * context.getTasks().size());
    });

    static LinkContext getContext(Channel channel)
    {
        return channel.attr(Vars.LINKCONTEXT_KEY).get();
    }

    private final LinkManager manager;
    private final Channel channel;
    private final EventExecutor executor;
    private final RoundTripTimeMeasurement RTTM;
    private final SmoothedRoundTripTime SRTT;
    private final Map<Integer, Runnable> tasks;
    private volatile LinkSession session;

    LinkContext(LinkManager manager, Channel channel)
    {
        this.manager = manager;
        this.channel = channel;
        this.executor = channel.eventLoop();
        this.RTTM = new RoundTripTimeMeasurement();
        this.SRTT = new SmoothedRoundTripTime();
        this.tasks = new ConcurrentHashMap<>();
    }

    ChannelFuture close()
    {
        return channel.close();
    }

    Channel getChannel()
    {
        return channel;
    }

    EventExecutor getExecutor()
    {
        return executor;
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

    Map<Integer, Runnable> getTasks()
    {
        return tasks;
    }

    boolean isActive()
    {
        return channel.isActive();
    }

    LinkContext setSession(LinkSession session)
    {
        this.session = session;
        return this;
    }

    void tick()
    {
        if (isActive())
        {
            if (getExecutor().inEventLoop())
                tick0();
            else
                getExecutor().execute(() -> tick0());
        }
    }

    private void tick0()
    {
        getRTTM().updateIf(rtt -> rtt >= 2000L).ifPresent(getSRTT()::updateAndGet);
    }

    ChannelFuture writeAndFlush(Object msg)
    {
        return channel.writeAndFlush(msg);
    }

}

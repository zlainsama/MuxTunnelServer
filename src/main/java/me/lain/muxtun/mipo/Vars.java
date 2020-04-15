package me.lain.muxtun.mipo;

import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import me.lain.muxtun.Shared;

class Vars
{

    static final class ChannelError
    {

        static void accumulate(Channel channel, Throwable error)
        {
            if (channel.attr(Vars.ERROR_KEY).get() != null || !channel.attr(Vars.ERROR_KEY).compareAndSet(null, error))
                channel.attr(Vars.ERROR_KEY).get().addSuppressed(error);
        }

        static Throwable accumulateAndGet(Channel channel, Throwable error)
        {
            accumulate(channel, error);

            return get(channel);
        }

        static Throwable get(Channel channel)
        {
            return channel.attr(Vars.ERROR_KEY).get();
        }

        static Throwable remove(Channel channel)
        {
            return channel.attr(Vars.ERROR_KEY).getAndSet(null);
        }

        private ChannelError()
        {
        }

    }

    static final AttributeKey<Throwable> ERROR_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#Error");
    static final AttributeKey<LinkContext> LINKCONTEXT_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#LinkContext");
    static final AttributeKey<StreamContext> STREAMCONTEXT_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#StreamContext");

    static final Timer TIMER = new HashedWheelTimer(new DefaultThreadFactory("timer", true));

    static final EventLoopGroup BOSS = Shared.NettyObjects.getOrCreateEventLoopGroup("bossGroup", 1);
    static final EventLoopGroup LINKS = Shared.NettyObjects.getOrCreateEventLoopGroup("linksGroup", 8);
    static final EventLoopGroup STREAMS = Shared.NettyObjects.getOrCreateEventLoopGroup("streamsGroup", 8);
    static final EventExecutorGroup SESSIONS = Shared.NettyObjects.getOrCreateEventExecutorGroup("sessionsGroup", 8);

}

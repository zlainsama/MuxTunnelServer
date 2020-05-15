package me.lain.muxtun.mipo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.util.HashedWheelTimer;
import io.netty.util.Timer;
import io.netty.util.concurrent.DefaultThreadFactory;
import me.lain.muxtun.Shared;
import me.lain.muxtun.codec.Message;

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

    static final ByteBuf TRUE_BUFFER = Unpooled.unreleasableBuffer(Unpooled.directBuffer(1, 1).writeBoolean(true).asReadOnly());
    static final ByteBuf FALSE_BUFFER = Unpooled.unreleasableBuffer(Unpooled.directBuffer(1, 1).writeBoolean(false).asReadOnly());

    static final AttributeKey<Throwable> ERROR_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#Error");
    static final AttributeKey<LinkContext> LINKCONTEXT_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#LinkContext");
    static final AttributeKey<StreamContext> STREAMCONTEXT_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#StreamContext");

    static final Timer TIMER = new HashedWheelTimer(new DefaultThreadFactory("timer", true));

    static final int NUMTHREADS = Math.max(4, Math.min(Runtime.getRuntime().availableProcessors(), Short.MAX_VALUE));
    static final EventLoopGroup WORKERS = Shared.NettyObjects.getOrCreateEventLoopGroup("workersGroup", NUMTHREADS);

    static final Message PLACEHOLDER = new Message()
    {

        @Override
        public Message copy()
        {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public void decode(ByteBuf buf) throws Exception
        {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public void encode(ByteBuf buf) throws Exception
        {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public int size()
        {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public MessageType type()
        {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

    };

}

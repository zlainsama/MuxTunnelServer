package me.lain.muxtun.mipo;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import io.netty.channel.Channel;

interface StreamContext
{

    class DefaultStreamContext implements StreamContext
    {

        private final Channel channel;

        DefaultStreamContext(Channel channel)
        {
            this.channel = channel;
        }

        @Override
        public Channel getChannel()
        {
            return channel;
        }

        @Override
        public int updateWindowSize(IntUnaryOperator updateFunction)
        {
            channel.config().setAutoRead(true);
            return getWindowSize();
        }

    }

    class FlowControlledStreamContext implements StreamContext
    {

        private final Channel channel;
        private final AtomicInteger window;
        private final AtomicInteger received;
        private final AtomicInteger threshold;
        private final AtomicInteger increment;

        FlowControlledStreamContext(Channel channel)
        {
            this(channel, 65536);
        }

        FlowControlledStreamContext(Channel channel, int initialWindowSize)
        {
            this.channel = channel;
            this.window = new AtomicInteger(initialWindowSize);
            this.received = new AtomicInteger(0);
            this.threshold = new AtomicInteger(32768);
            this.increment = new AtomicInteger(65536);
        }

        @Override
        public Channel getChannel()
        {
            return channel;
        }

        @Override
        public int getNextIncrement()
        {
            return increment.get();
        }

        @Override
        public int getReceived()
        {
            return received.get();
        }

        @Override
        public int getThreshold()
        {
            return threshold.get();
        }

        @Override
        public int getWindowSize()
        {
            return window.get();
        }

        @Override
        public int updateNextIncrement(IntUnaryOperator updateFunction)
        {
            return increment.updateAndGet(updateFunction);
        }

        @Override
        public int updateReceived(IntUnaryOperator updateFunction)
        {
            return received.updateAndGet(updateFunction);
        }

        @Override
        public int updateThreshold(IntUnaryOperator updateFunction)
        {
            return threshold.updateAndGet(updateFunction);
        }

        @Override
        public int updateWindowSize(IntUnaryOperator updateFunction)
        {
            int result = window.updateAndGet(updateFunction);
            channel.config().setAutoRead(result > 0);
            return result;
        }

    }

    @SuppressWarnings("unchecked")
    default <T extends StreamContext> T cast()
    {
        return (T) this;
    }

    default void close()
    {
        getChannel().close();
    }

    Channel getChannel();

    default int getNextIncrement()
    {
        return 65536;
    }

    default int getReceived()
    {
        return 0;
    }

    default int getThreshold()
    {
        return 32768;
    }

    default int getWindowSize()
    {
        return 65536;
    }

    default boolean isActive()
    {
        return getChannel().isActive();
    }

    default int updateNextIncrement(IntUnaryOperator updateFunction)
    {
        return getNextIncrement();
    }

    default int updateReceived(IntUnaryOperator updateFunction)
    {
        return getReceived();
    }

    default int updateThreshold(IntUnaryOperator updateFunction)
    {
        return getThreshold();
    }

    default int updateWindowSize(IntUnaryOperator updateFunction)
    {
        return getWindowSize();
    }

    default void writeAndFlush(Object msg) throws Exception
    {
        getChannel().writeAndFlush(msg);
    }

}

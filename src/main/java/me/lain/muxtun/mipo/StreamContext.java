package me.lain.muxtun.mipo;

import java.util.Optional;
import java.util.UUID;
import java.util.function.IntUnaryOperator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;

class StreamContext
{

    private final UUID streamId;
    private final Channel channel;
    private final Optional<FlowControl> flowControl;

    StreamContext(UUID streamId, Channel channel, FlowControl flowControl)
    {
        this.streamId = streamId;
        this.channel = channel;
        this.flowControl = Optional.ofNullable(flowControl);
    }

    ChannelFuture close()
    {
        return channel.close();
    }

    Channel getChannel()
    {
        return channel;
    }

    Optional<FlowControl> getFlowControl()
    {
        return flowControl;
    }

    UUID getStreamId()
    {
        return streamId;
    }

    boolean isActive()
    {
        return channel.isActive();
    }

    int updateReceived(IntUnaryOperator updateFunction)
    {
        return flowControl.map(fc -> fc.updateReceived(updateFunction)).orElse(0);
    }

    int updateWindowSize(IntUnaryOperator updateFunction)
    {
        return flowControl.map(fc -> fc.updateWindowSize(updateFunction)).orElse(65536);
    }

    ChannelFuture writeAndFlush(Object msg) throws Exception
    {
        return channel.writeAndFlush(msg);
    }

}

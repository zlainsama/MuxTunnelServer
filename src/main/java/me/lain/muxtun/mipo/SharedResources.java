package me.lain.muxtun.mipo;

import io.netty.channel.ChannelFutureListener;

import java.net.SocketAddress;
import java.util.UUID;
import java.util.function.Function;

class SharedResources {

    private final ChannelFutureListener channelAccumulator;
    private final Function<UUID, SocketAddress> targetTableLookup;

    SharedResources(ChannelFutureListener channelAccumulator, Function<UUID, SocketAddress> targetTableLookup) {
        this.channelAccumulator = channelAccumulator;
        this.targetTableLookup = targetTableLookup;
    }

    ChannelFutureListener getChannelAccumulator() {
        return channelAccumulator;
    }

    Function<UUID, SocketAddress> getTargetTableLookup() {
        return targetTableLookup;
    }

}

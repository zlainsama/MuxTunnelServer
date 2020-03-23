package me.lain.muxtun.mipo;

import java.net.SocketAddress;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.group.ChannelGroup;
import me.lain.muxtun.Shared;

class LinkSession
{

    final ChannelFutureListener channelAccumulator;
    final Function<UUID, Optional<SocketAddress>> targetTableLookup;
    final Function<byte[], Optional<byte[]>> challengeGenerator;
    final Function<byte[], Optional<byte[]>> challengeGenerator_3;
    final Map<UUID, StreamContext> ongoingStreams;
    final LinkSessionAuthStatus authStatus;
    final AtomicBoolean flowControl;

    LinkSession(MirrorPointConfig config, ChannelGroup channels)
    {
        channelAccumulator = future -> {
            if (future.isSuccess())
                channels.add(future.channel());
        };
        targetTableLookup = requestId -> {
            return Optional.ofNullable(config.targetAddresses.get(requestId));
        };
        challengeGenerator = question -> {
            if (config.secret != null)
                return Optional.of(Shared.digestSHA256(2, config.secret, question));
            return Optional.empty();
        };
        challengeGenerator_3 = question -> {
            if (config.secret_3 != null && Shared.isSHA3Available())
                return Optional.of(Shared.digestSHA256_3(2, config.secret_3, question));
            return Optional.empty();
        };
        ongoingStreams = new ConcurrentHashMap<>();
        authStatus = new LinkSessionAuthStatus();
        flowControl = new AtomicBoolean();
    }

}

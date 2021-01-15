package me.lain.muxtun.mipo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.lain.muxtun.Shared;
import me.lain.muxtun.codec.MessageCodec;
import me.lain.muxtun.mipo.config.MirrorPointConfig;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class MirrorPoint {

    private final MirrorPointConfig config;
    private final SslContext sslCtx;
    private final ChannelGroup channels;
    private final LinkManager manager;
    private final AtomicReference<Future<?>> scheduledMaintainTask;

    public MirrorPoint(MirrorPointConfig config) throws IOException {
        this.config = Objects.requireNonNull(config, "config");
        this.sslCtx = MirrorPointConfig.buildContext(config.getPathCert(), config.getPathKey(), config.getTrusts(), config.getCiphers(), config.getProtocols());
        this.channels = new DefaultChannelGroup("MirrorPoint", GlobalEventExecutor.INSTANCE, true);
        this.manager = new LinkManager(new SharedResources(future -> {
            if (future.isSuccess())
                channels.add(future.channel());
        }, requestId -> {
            return Optional.ofNullable(config.getTargetAddresses().get(requestId));
        }));
        this.scheduledMaintainTask = new AtomicReference<>();
    }

    public ChannelGroup getChannels() {
        return channels;
    }

    public Future<?> start() {
        return new ServerBootstrap()
                .group(Vars.BOSSES, Vars.WORKERS)
                .channel(Shared.NettyObjects.classServerSocketChannel)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ch.attr(Vars.LINKCONTEXT_KEY).set(new LinkContext(manager, ch));
                        ch.newSucceededFuture().addListener(manager.getResources().getChannelAccumulator());

                        ch.pipeline().addLast(new ReadTimeoutHandler(600));
                        ch.pipeline().addLast(new WriteTimeoutHandler(60));
                        ch.pipeline().addLast(Vars.HANDLERNAME_TLS, sslCtx.newHandler(ch.alloc(), Vars.SHARED_POOL));
                        ch.pipeline().addLast(Vars.HANDLERNAME_CODEC, new MessageCodec());
                        ch.pipeline().addLast(Vars.HANDLERNAME_HANDLER, LinkHandler.DEFAULT);
                    }

                })
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .bind(config.getBindAddress())
                .addListener(manager.getResources().getChannelAccumulator())
                .addListener(future -> {
                    if (future.isSuccess())
                        Optional.ofNullable(scheduledMaintainTask.getAndSet(GlobalEventExecutor.INSTANCE.scheduleWithFixedDelay(() -> {
                            manager.getSessions().values().forEach(LinkSession::tick);
                        }, 1L, 1L, TimeUnit.SECONDS))).ifPresent(scheduled -> scheduled.cancel(false));
                    else
                        stop();
                });
    }

    public Future<?> stop() {
        return channels.close().addListener(future -> {
            manager.getSessions().values().forEach(LinkSession::close);
            Optional.ofNullable(scheduledMaintainTask.getAndSet(null)).ifPresent(scheduled -> scheduled.cancel(false));
        });
    }

}

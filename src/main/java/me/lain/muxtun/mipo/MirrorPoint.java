package me.lain.muxtun.mipo;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.lain.muxtun.Shared;
import me.lain.muxtun.codec.MessageCodec;
import me.lain.muxtun.util.SimpleLogger;

public class MirrorPoint
{

    private final MirrorPointConfig config;
    private final ChannelGroup channels;
    private final LinkManager manager;
    private final AtomicReference<Future<?>> scheduledMaintainTask;

    public MirrorPoint(MirrorPointConfig config)
    {
        this.config = config;
        this.channels = new DefaultChannelGroup("MirrorPoint", GlobalEventExecutor.INSTANCE, true);
        this.manager = new LinkManager(new SharedResources(future -> {
            if (future.isSuccess())
                channels.add(future.channel());
        }, requestId -> {
            return Optional.ofNullable(config.getTargetAddresses().get(requestId));
        }));
        this.scheduledMaintainTask = new AtomicReference<>();
    }

    public ChannelGroup getChannels()
    {
        return channels;
    }

    public Future<?> start()
    {
        return new ServerBootstrap()
                .group(Vars.WORKERS)
                .channel(Shared.NettyObjects.classServerSocketChannel)
                .childHandler(new ChannelInitializer<SocketChannel>()
                {

                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception
                    {
                        ch.attr(Vars.LINKCONTEXT_KEY).set(new LinkContext(manager, ch));
                        ch.newSucceededFuture().addListener(manager.getResources().getChannelAccumulator());
                        ch.closeFuture().addListener(future -> {
                            ch.eventLoop().execute(() -> {
                                Throwable error = Vars.ChannelError.get(ch);
                                if (error != null)
                                {
                                    SimpleLogger.printStackTrace(error);
                                    SimpleLogger.println("%s > [%s] link %s closed with unexpected error. (%s)", Shared.printNow(), config.getName(), ch.id(), error);
                                }
                            });
                        });

                        ch.pipeline().addLast(new ReadTimeoutHandler(600));
                        ch.pipeline().addLast(new WriteTimeoutHandler(60));
                        ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
                        ch.pipeline().addLast(config.getSslCtx().newHandler(ch.alloc()));
                        ch.pipeline().addLast(new MessageCodec());
                        ch.pipeline().addLast(LinkHandler.DEFAULT);
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

    public Future<?> stop()
    {
        return channels.close().addListener(future -> {
            manager.getSessions().values().forEach(LinkSession::close);
            Optional.ofNullable(scheduledMaintainTask.getAndSet(null)).ifPresent(scheduled -> scheduled.cancel(false));
        });
    }

    @Override
    public String toString()
    {
        return config.getName();
    }

}

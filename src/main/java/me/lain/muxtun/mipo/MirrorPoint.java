package me.lain.muxtun.mipo;

import java.util.Optional;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.lain.muxtun.Shared;
import me.lain.muxtun.codec.FrameCodec;
import me.lain.muxtun.codec.MessageCodec;
import me.lain.muxtun.codec.SnappyCodec;
import me.lain.muxtun.util.SimpleLogger;

public class MirrorPoint
{

    private final MirrorPointConfig config;
    private final ChannelGroup channels;
    private final LinkManager manager;

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
    }

    public ChannelGroup getChannels()
    {
        return channels;
    }

    public Future<?> start()
    {
        return new ServerBootstrap()
                .group(Vars.BOSS, Vars.LINKS)
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
                                    SimpleLogger.println("%s > [%s] link %s closed with unexpected error. (%s)", Shared.printNow(), config.getName(), ch.id(), error);
                            });
                        });

                        ch.pipeline().addLast(new ReadTimeoutHandler(600));
                        ch.pipeline().addLast(new WriteTimeoutHandler(60));
                        ch.pipeline().addLast(config.getSslCtx().newHandler(ch.alloc()));
                        ch.pipeline().addLast(new ChunkedWriteHandler());
                        ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
                        ch.pipeline().addLast(new SnappyCodec());
                        ch.pipeline().addLast(new FrameCodec());
                        ch.pipeline().addLast(MessageCodec.DEFAULT);
                        ch.pipeline().addLast(LinkHandler.DEFAULT);
                    }

                })
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .bind(config.getBindAddress())
                .addListener(manager.getResources().getChannelAccumulator());
    }

    public Future<?> stop()
    {
        return channels.close();
    }

    @Override
    public String toString()
    {
        return config.getName();
    }

}

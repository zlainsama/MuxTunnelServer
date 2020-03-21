package me.lain.muxtun.mipo;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.lain.muxtun.Shared;

public class MirrorPoint
{

    private final MirrorPointConfig config;
    private final ChannelGroup channels;
    private final LinkInitializer linkInitializer;

    public MirrorPoint(MirrorPointConfig config)
    {
        this.config = config;
        this.channels = new DefaultChannelGroup("MirrorPoint", GlobalEventExecutor.INSTANCE, true);
        this.linkInitializer = new LinkInitializer(config, channels);
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
                .childHandler(linkInitializer)
                .option(ChannelOption.SO_BACKLOG, 1024)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .bind(config.bindAddress)
                .addListener(new ChannelFutureListener()
                {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception
                    {
                        if (future.isSuccess())
                            channels.add(future.channel());
                    }

                });
    }

    public Future<?> stop()
    {
        return channels.close();
    }

    @Override
    public String toString()
    {
        return config.name;
    }

}

package me.lain.muxtun.mipo;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import me.lain.muxtun.Shared;
import me.lain.muxtun.codec.FrameCodec;
import me.lain.muxtun.codec.MessageCodec;
import me.lain.muxtun.util.SimpleLogger;

@Sharable
class LinkInitializer extends ChannelInitializer<SocketChannel>
{

    private static final ChannelFutureListener CLOSESTREAMS = future -> {
        future.channel().attr(Vars.SESSION_KEY).get().ongoingStreams.values().forEach(StreamContext::close);
    };

    private final MirrorPointConfig config;
    private final ChannelGroup channels;

    LinkInitializer(MirrorPointConfig config, ChannelGroup channels)
    {
        this.config = config;
        this.channels = channels;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception
    {
        ch.attr(Vars.SESSION_KEY).set(new LinkSession(config, channels));
        channels.add(ch);
        ch.closeFuture().addListener(CLOSESTREAMS).addListener(future -> {
            ch.eventLoop().execute(() -> {
                LinkSession session = ch.attr(Vars.SESSION_KEY).get();
                Throwable error = ch.attr(Vars.ERROR_KEY).get();
                if (session.ongoingStreams.size() > 0 && error != null)
                    SimpleLogger.println("%s > [%s] link %s closed with unexpected error. (%d dropped) (%s)", Shared.printNow(), config.name, ch.id(), session.ongoingStreams.size(), error);
            });
        });

        ch.pipeline().addLast(new ReadTimeoutHandler(600));
        ch.pipeline().addLast(new WriteTimeoutHandler(60));
        ch.pipeline().addLast(new ChunkedWriteHandler());
        ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
        ch.pipeline().addLast(config.sslCtx.newHandler(ch.alloc()));
        ch.pipeline().addLast("FrameCodec", new FrameCodec());
        ch.pipeline().addLast("MessageCodec", MessageCodec.DEFAULT);
        ch.pipeline().addLast(LinkInboundHandler.DEFAULT);
        ch.pipeline().addLast(LinkWritabilityChangeListener.DEFAULT);
        ch.pipeline().addLast(LinkExceptionHandler.DEFAULT);
    }

}

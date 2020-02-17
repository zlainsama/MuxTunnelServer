package me.lain.muxtun;

import java.net.SocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.GlobalEventExecutor;
import me.lain.muxtun.codec.FrameCodec;
import me.lain.muxtun.codec.Message;
import me.lain.muxtun.codec.Message.MessageType;
import me.lain.muxtun.codec.MessageCodec;

public class MirrorPoint
{

    private static final AttributeKey<Map<UUID, Channel>> ONGOINGSTREAMS_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#OngoingStreams");
    private static final AttributeKey<UUID> STREAMID_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#StreamId");
    private static final AttributeKey<Channel> BOUNDCHANNEL_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#BoundChannel");

    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final String identifier;

    public MirrorPoint(final SocketAddress bindAddress, final Map<UUID, SocketAddress> targetAddresses, final SslContext sslCtx)
    {
        this(bindAddress, targetAddresses, sslCtx, "MirrorPoint");
    }

    public MirrorPoint(final SocketAddress bindAddress, final Map<UUID, SocketAddress> targetAddresses, final SslContext sslCtx, final String name)
    {
        if (bindAddress == null || targetAddresses == null || sslCtx == null || name == null)
            throw new NullPointerException();
        if (targetAddresses.isEmpty() || !sslCtx.isServer() || name.isEmpty())
            throw new IllegalArgumentException();

        identifier = name;
        allChannels.add(new ServerBootstrap().group(Shared.bossGroup, Shared.workerGroup).channel(Shared.classServerSocketChannel).childHandler(new ChannelInitializer<SocketChannel>()
        {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception
            {
                ch.pipeline().addLast(new FlushConsolidationHandler());
                ch.pipeline().addLast(new ReadTimeoutHandler(60));
                ch.pipeline().addLast(new WriteTimeoutHandler(5));
                ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                ch.pipeline().addLast(new FrameCodec());
                ch.pipeline().addLast(new MessageCodec());
                ch.pipeline().addLast(new SimpleChannelInboundHandler<Message>()
                {

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        channel.attr(ONGOINGSTREAMS_KEY).set(new ConcurrentHashMap<>());

                        allChannels.add(channel);
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        channel.attr(ONGOINGSTREAMS_KEY).get().values().forEach(Channel::close);
                    }

                    @Override
                    protected void channelRead0(ChannelHandlerContext ctx, Message msg) throws Exception
                    {
                        try
                        {
                            final Channel channel = ctx.channel();

                            if (channel.isActive())
                            {
                                switch (msg.getType())
                                {
                                    case Ping:
                                    {
                                        channel.writeAndFlush(new Message()
                                                .setType(MessageType.Ping));
                                        break;
                                    }
                                    case Open:
                                    {
                                        final UUID requestId = msg.getStreamId();

                                        final SocketAddress toOpen = targetAddresses.get(requestId);
                                        if (toOpen != null)
                                            new Bootstrap().group(Shared.workerGroup).channel(Shared.classSocketChannel).handler(new ChannelInitializer<SocketChannel>()
                                            {

                                                @Override
                                                protected void initChannel(SocketChannel ch) throws Exception
                                                {
                                                    ch.pipeline().addLast(new FlushConsolidationHandler());
                                                    ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>()
                                                    {

                                                        @Override
                                                        public void channelActive(ChannelHandlerContext ctx) throws Exception
                                                        {
                                                            final Channel boundChannel = channel;
                                                            final Channel channel = ctx.channel();
                                                            final UUID streamId = UUID.randomUUID();

                                                            channel.attr(STREAMID_KEY).set(streamId);
                                                            channel.attr(BOUNDCHANNEL_KEY).set(boundChannel);
                                                            boundChannel.attr(ONGOINGSTREAMS_KEY).get().put(streamId, channel);
                                                            allChannels.add(channel);

                                                            if (boundChannel.isActive() && boundChannel.attr(ONGOINGSTREAMS_KEY).get().get(streamId) == channel)
                                                                boundChannel.writeAndFlush(new Message()
                                                                        .setType(MessageType.Open)
                                                                        .setStreamId(streamId));
                                                            else
                                                                channel.close();
                                                        }

                                                        @Override
                                                        public void channelInactive(ChannelHandlerContext ctx) throws Exception
                                                        {
                                                            final Channel channel = ctx.channel();
                                                            final UUID streamId = channel.attr(STREAMID_KEY).get();
                                                            final Channel boundChannel = channel.attr(BOUNDCHANNEL_KEY).get();

                                                            if (boundChannel.isActive() && boundChannel.attr(ONGOINGSTREAMS_KEY).get().remove(streamId) == channel)
                                                                boundChannel.writeAndFlush(new Message()
                                                                        .setType(MessageType.Drop)
                                                                        .setStreamId(streamId));
                                                        }

                                                        @Override
                                                        protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception
                                                        {
                                                            final Channel channel = ctx.channel();
                                                            final UUID streamId = channel.attr(STREAMID_KEY).get();
                                                            final Channel boundChannel = channel.attr(BOUNDCHANNEL_KEY).get();

                                                            if (boundChannel.isActive() && boundChannel.attr(ONGOINGSTREAMS_KEY).get().get(streamId) == channel)
                                                                boundChannel.writeAndFlush(new Message()
                                                                        .setType(MessageType.Data)
                                                                        .setStreamId(streamId)
                                                                        .setPayload(msg.retainedDuplicate()));
                                                            else
                                                                channel.close();
                                                        }

                                                        @Override
                                                        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
                                                        {
                                                            final Channel subChannel = ctx.channel();

                                                            subChannel.close();
                                                        }

                                                    });
                                                }

                                            }).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true).connect(toOpen).addListener(future -> {
                                                if (!future.isSuccess())
                                                    channel.writeAndFlush(new Message()
                                                            .setType(MessageType.Drop)
                                                            .setStreamId(requestId));
                                            });
                                        else
                                            channel.writeAndFlush(new Message()
                                                    .setType(MessageType.Drop)
                                                    .setStreamId(requestId));
                                        break;
                                    }
                                    case Data:
                                    {
                                        final UUID streamId = msg.getStreamId();
                                        final ByteBuf payload = msg.getPayload();

                                        final Channel toSend = channel.attr(ONGOINGSTREAMS_KEY).get().get(streamId);
                                        if (toSend != null && toSend.isActive())
                                            toSend.writeAndFlush(payload.retainedDuplicate());
                                        else
                                            channel.writeAndFlush(new Message()
                                                    .setType(MessageType.Drop)
                                                    .setStreamId(streamId));
                                        break;
                                    }
                                    case Drop:
                                    {
                                        final UUID streamId = msg.getStreamId();

                                        final Channel toClose = channel.attr(ONGOINGSTREAMS_KEY).get().remove(streamId);
                                        if (toClose != null && toClose.isActive())
                                            toClose.close();
                                        break;
                                    }
                                    default:
                                    {
                                        channel.close();
                                        break;
                                    }
                                }
                            }
                        }
                        finally
                        {
                            ReferenceCountUtil.release(msg.getPayload());
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        channel.close();
                    }

                });
            }

        }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.TCP_NODELAY, true).bind(bindAddress).syncUninterruptibly().channel());
    }

    public ChannelGroup getChannels()
    {
        return allChannels;
    }

    @Override
    public String toString()
    {
        return identifier;
    }

}

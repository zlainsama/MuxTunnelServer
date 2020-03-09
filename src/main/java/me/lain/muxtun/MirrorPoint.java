package me.lain.muxtun;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.socket.DatagramChannel;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.flush.FlushConsolidationHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
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

    private static final AttributeKey<Throwable> CHANNELERROR_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#ChannelError");
    private static final AttributeKey<Map<UUID, Channel>> ONGOINGSTREAMS_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#OngoingStreams");
    private static final AttributeKey<Optional<Boolean>> AUTHSTATUS_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#AuthStatus");
    private static final AttributeKey<Optional<byte[]>> CHALLENGE_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#Challenge");
    private static final AttributeKey<UUID> STREAMID_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#StreamId");
    private static final AttributeKey<Channel> BOUNDCHANNEL_KEY = AttributeKey.newInstance("me.lain.muxtun.MirrorPoint#BoundChannel");

    private final ChannelGroup allChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    private final String identifier;

    public MirrorPoint(final SocketAddress bindAddress, final Map<UUID, SocketAddress> targetAddresses, final SslContext sslCtx, final Optional<byte[]> secret, final Optional<byte[]> secret_3)
    {
        this(bindAddress, targetAddresses, sslCtx, secret, secret_3, "MirrorPoint");
    }

    public MirrorPoint(final SocketAddress bindAddress, final Map<UUID, SocketAddress> targetAddresses, final SslContext sslCtx, final Optional<byte[]> secret, final Optional<byte[]> secret_3, final String name)
    {
        if (bindAddress == null || targetAddresses == null || sslCtx == null || secret == null || secret_3 == null || name == null)
            throw new NullPointerException();
        if (targetAddresses.isEmpty() || !sslCtx.isServer() || (!secret.isPresent() && !secret_3.isPresent()) || name.isEmpty())
            throw new IllegalArgumentException();

        identifier = name;
        allChannels.add(new ServerBootstrap().group(Shared.NettyObjects.bossGroup, Shared.NettyObjects.workerGroup).channel(Shared.NettyObjects.classServerSocketChannel).childHandler(new ChannelInitializer<SocketChannel>()
        {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception
            {
                ch.pipeline().addLast(new ReadTimeoutHandler(600));
                ch.pipeline().addLast(new WriteTimeoutHandler(60));
                ch.pipeline().addLast(new ChunkedWriteHandler());
                ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
                ch.pipeline().addLast(sslCtx.newHandler(ch.alloc()));
                ch.pipeline().addLast(new FrameCodec());
                ch.pipeline().addLast(MessageCodec.DEFAULT);
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter()
                {

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        channel.attr(ONGOINGSTREAMS_KEY).set(new ConcurrentHashMap<>());
                        channel.attr(AUTHSTATUS_KEY).set(Optional.empty());
                        channel.attr(CHALLENGE_KEY).set(Optional.empty());

                        allChannels.add(channel);
                    }

                    @Override
                    public void channelInactive(ChannelHandlerContext ctx) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        channel.attr(ONGOINGSTREAMS_KEY).get().values().forEach(Channel::close);

                        channel.eventLoop().submit(() -> {
                            if (!channel.attr(ONGOINGSTREAMS_KEY).get().isEmpty() && channel.attr(CHANNELERROR_KEY).get() != null)
                                System.out.println(String.format("%s > [%s] link %s closed with unexpected error. (%d dropped) (%s)", Shared.printNow(), identifier, channel.id(), channel.attr(ONGOINGSTREAMS_KEY).get().size(), channel.attr(CHANNELERROR_KEY).get().toString()));
                        });
                    }

                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
                    {
                        if (msg instanceof Message)
                        {
                            Message cast = (Message) msg;

                            try
                            {
                                handleMessage(ctx, cast);
                            }
                            finally
                            {
                                ReferenceCountUtil.release(cast.getPayload());
                            }
                        }
                        else
                        {
                            try
                            {
                                ctx.close();
                            }
                            finally
                            {
                                ReferenceCountUtil.release(msg);
                            }
                        }
                    }

                    @Override
                    public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception
                    {
                        final Channel channel = ctx.channel();
                        final boolean writable = channel.isWritable();

                        channel.attr(ONGOINGSTREAMS_KEY).get().values().forEach(stream -> stream.config().setAutoRead(writable));
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        if (channel.attr(CHANNELERROR_KEY).get() != null || !channel.attr(CHANNELERROR_KEY).compareAndSet(null, cause))
                            channel.attr(CHANNELERROR_KEY).get().addSuppressed(cause);

                        channel.close();
                    }

                    private void handleMessage(ChannelHandlerContext ctx, Message msg) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        if (channel.isActive())
                        {
                            switch (msg.getType())
                            {
                                case Ping:
                                {
                                    if (channel.attr(AUTHSTATUS_KEY).get().orElse(false))
                                    {
                                        channel.writeAndFlush(new Message()
                                                .setType(MessageType.Ping));
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
                                    break;
                                }
                                case Open:
                                {
                                    if (channel.attr(AUTHSTATUS_KEY).get().orElse(false))
                                    {
                                        final UUID requestId = msg.getStreamId();

                                        final SocketAddress toOpen = targetAddresses.get(requestId);
                                        if (toOpen != null)
                                            open(toOpen, channel).addListener(future -> {
                                                if (!future.isSuccess())
                                                    channel.writeAndFlush(new Message()
                                                            .setType(MessageType.Drop)
                                                            .setStreamId(requestId));
                                            });
                                        else
                                            channel.writeAndFlush(new Message()
                                                    .setType(MessageType.Drop)
                                                    .setStreamId(requestId))
                                                    .addListener(ChannelFutureListener.CLOSE);
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
                                    break;
                                }
                                case Data:
                                {
                                    if (channel.attr(AUTHSTATUS_KEY).get().orElse(false))
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
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
                                    break;
                                }
                                case Drop:
                                {
                                    if (channel.attr(AUTHSTATUS_KEY).get().orElse(false))
                                    {
                                        final UUID streamId = msg.getStreamId();

                                        final Channel toClose = channel.attr(ONGOINGSTREAMS_KEY).get().remove(streamId);
                                        if (toClose != null && toClose.isActive())
                                            toClose.close();
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
                                    break;
                                }
                                case OpenUDP:
                                {
                                    if (channel.attr(AUTHSTATUS_KEY).get().orElse(false))
                                    {
                                        final UUID requestId = msg.getStreamId();

                                        final SocketAddress toOpen = targetAddresses.get(requestId);
                                        if (toOpen != null)
                                            openUDP(toOpen, channel).addListener(future -> {
                                                if (!future.isSuccess())
                                                    channel.writeAndFlush(new Message()
                                                            .setType(MessageType.Drop)
                                                            .setStreamId(requestId));
                                            });
                                        else
                                            channel.writeAndFlush(new Message()
                                                    .setType(MessageType.Drop)
                                                    .setStreamId(requestId))
                                                    .addListener(ChannelFutureListener.CLOSE);
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
                                    break;
                                }
                                case Auth:
                                {
                                    if (!channel.attr(AUTHSTATUS_KEY).get().isPresent() && channel.attr(CHALLENGE_KEY).get().isPresent())
                                    {
                                        final ByteBuf payload = msg.getPayload();

                                        if (Arrays.equals(channel.attr(CHALLENGE_KEY).get().orElse(null), ByteBufUtil.getBytes(payload, payload.readerIndex(), payload.readableBytes(), false)))
                                        {
                                            if (channel.attr(AUTHSTATUS_KEY).compareAndSet(Optional.empty(), Optional.of(true)))
                                            {
                                                channel.attr(CHALLENGE_KEY).set(Optional.empty());
                                                channel.writeAndFlush(new Message()
                                                        .setType(MessageType.Auth)
                                                        .setPayload(Unpooled.EMPTY_BUFFER));
                                            }
                                        }
                                        else
                                        {
                                            if (channel.attr(AUTHSTATUS_KEY).compareAndSet(Optional.empty(), Optional.of(false)))
                                            {
                                                channel.attr(CHALLENGE_KEY).set(Optional.empty());
                                                channel.close();
                                            }
                                        }
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
                                    break;
                                }
                                case AuthReq:
                                {
                                    if (!channel.attr(AUTHSTATUS_KEY).get().isPresent() && !channel.attr(CHALLENGE_KEY).get().isPresent())
                                    {
                                        final ByteBuf buf = Unpooled.buffer(16, 16);

                                        try
                                        {
                                            final UUID id = UUID.randomUUID();

                                            buf.writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits());
                                            if (secret.isPresent())
                                            {
                                                final byte[] digest = Shared.digestSHA256(2, secret.get(), ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false));

                                                if (channel.attr(CHALLENGE_KEY).compareAndSet(Optional.empty(), Optional.of(digest)))
                                                {
                                                    channel.writeAndFlush(new Message()
                                                            .setType(MessageType.AuthReq)
                                                            .setPayload(buf.retain()));
                                                }
                                            }
                                            else if (secret_3.isPresent() && Shared.isSHA3Available())
                                            {
                                                final byte[] digest = Shared.digestSHA256_3(2, secret_3.get(), ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false));

                                                if (channel.attr(CHALLENGE_KEY).compareAndSet(Optional.empty(), Optional.of(digest)))
                                                {
                                                    channel.writeAndFlush(new Message()
                                                            .setType(MessageType.AuthReq_3)
                                                            .setPayload(buf.retain()));
                                                }
                                            }
                                            else
                                            {
                                                channel.close();
                                            }
                                        }
                                        finally
                                        {
                                            ReferenceCountUtil.release(buf);
                                        }
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
                                    break;
                                }
                                case AuthReq_3:
                                {
                                    if (!channel.attr(AUTHSTATUS_KEY).get().isPresent() && !channel.attr(CHALLENGE_KEY).get().isPresent())
                                    {
                                        final ByteBuf buf = Unpooled.buffer(16, 16);

                                        try
                                        {
                                            final UUID id = UUID.randomUUID();

                                            buf.writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits());
                                            if (secret_3.isPresent() && Shared.isSHA3Available())
                                            {
                                                final byte[] digest = Shared.digestSHA256_3(2, secret_3.get(), ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false));

                                                if (channel.attr(CHALLENGE_KEY).compareAndSet(Optional.empty(), Optional.of(digest)))
                                                {
                                                    channel.writeAndFlush(new Message()
                                                            .setType(MessageType.AuthReq_3)
                                                            .setPayload(buf.retain()));
                                                }
                                            }
                                            else if (secret.isPresent())
                                            {
                                                final byte[] digest = Shared.digestSHA256(2, secret.get(), ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false));

                                                if (channel.attr(CHALLENGE_KEY).compareAndSet(Optional.empty(), Optional.of(digest)))
                                                {
                                                    channel.writeAndFlush(new Message()
                                                            .setType(MessageType.AuthReq)
                                                            .setPayload(buf.retain()));
                                                }
                                            }
                                            else
                                            {
                                                channel.close();
                                            }
                                        }
                                        finally
                                        {
                                            ReferenceCountUtil.release(buf);
                                        }
                                    }
                                    else
                                    {
                                        channel.close();
                                    }
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

                });
            }

        }).option(ChannelOption.SO_BACKLOG, 1024).childOption(ChannelOption.SO_KEEPALIVE, true).childOption(ChannelOption.TCP_NODELAY, true).bind(bindAddress).syncUninterruptibly().channel());
    }

    public ChannelGroup getChannels()
    {
        return allChannels;
    }

    private ChannelFuture open(final SocketAddress toOpen, final Channel parent)
    {
        return new Bootstrap().group(Shared.NettyObjects.workerGroup).channel(Shared.NettyObjects.classSocketChannel).handler(new ChannelInitializer<SocketChannel>()
        {

            @Override
            protected void initChannel(SocketChannel ch) throws Exception
            {
                ch.pipeline().addLast(new ChunkedWriteHandler());
                ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter()
                {

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception
                    {
                        final Channel boundChannel = parent;
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
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
                    {
                        if (msg instanceof ByteBuf)
                        {
                            ByteBuf cast = (ByteBuf) msg;

                            try
                            {
                                handleMessage(ctx, cast);
                            }
                            finally
                            {
                                ReferenceCountUtil.release(cast);
                            }
                        }
                        else
                        {
                            try
                            {
                                ctx.close();
                            }
                            finally
                            {
                                ReferenceCountUtil.release(msg);
                            }
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        if (channel.attr(CHANNELERROR_KEY).get() != null || !channel.attr(CHANNELERROR_KEY).compareAndSet(null, cause))
                            channel.attr(CHANNELERROR_KEY).get().addSuppressed(cause);

                        channel.close();
                    }

                    private void handleMessage(ChannelHandlerContext ctx, ByteBuf msg) throws Exception
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

                });
            }

        }).option(ChannelOption.SO_KEEPALIVE, true).option(ChannelOption.TCP_NODELAY, true).connect(toOpen);
    }

    private ChannelFuture openUDP(final SocketAddress toOpen, final Channel parent)
    {
        return new Bootstrap().group(Shared.NettyObjects.workerGroup).channel(Shared.NettyObjects.classDatagramChannel).handler(new ChannelInitializer<DatagramChannel>()
        {

            @Override
            protected void initChannel(DatagramChannel ch) throws Exception
            {
                ch.pipeline().addLast(new IdleStateHandler(0, 0, 60));
                ch.pipeline().addLast(new ChunkedWriteHandler());
                ch.pipeline().addLast(new FlushConsolidationHandler(64, true));
                ch.pipeline().addLast(new ChannelInboundHandlerAdapter()
                {

                    @Override
                    public void channelActive(ChannelHandlerContext ctx) throws Exception
                    {
                        final Channel boundChannel = parent;
                        final Channel channel = ctx.channel();
                        final UUID streamId = UUID.randomUUID();

                        channel.attr(STREAMID_KEY).set(streamId);
                        channel.attr(BOUNDCHANNEL_KEY).set(boundChannel);
                        boundChannel.attr(ONGOINGSTREAMS_KEY).get().put(streamId, channel);
                        allChannels.add(channel);

                        if (boundChannel.isActive() && boundChannel.attr(ONGOINGSTREAMS_KEY).get().get(streamId) == channel)
                            boundChannel.writeAndFlush(new Message()
                                    .setType(MessageType.OpenUDP)
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
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
                    {
                        if (msg instanceof DatagramPacket)
                        {
                            DatagramPacket cast = (DatagramPacket) msg;

                            try
                            {
                                handleMessage(ctx, cast);
                            }
                            finally
                            {
                                ReferenceCountUtil.release(cast);
                            }
                        }
                        else
                        {
                            try
                            {
                                ctx.close();
                            }
                            finally
                            {
                                ReferenceCountUtil.release(msg);
                            }
                        }
                    }

                    @Override
                    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
                    {
                        final Channel channel = ctx.channel();

                        if (channel.attr(CHANNELERROR_KEY).get() != null || !channel.attr(CHANNELERROR_KEY).compareAndSet(null, cause))
                            channel.attr(CHANNELERROR_KEY).get().addSuppressed(cause);

                        channel.close();
                    }

                    private void handleMessage(ChannelHandlerContext ctx, DatagramPacket msg) throws Exception
                    {
                        final Channel channel = ctx.channel();
                        final UUID streamId = channel.attr(STREAMID_KEY).get();
                        final Channel boundChannel = channel.attr(BOUNDCHANNEL_KEY).get();

                        if (boundChannel.isActive() && boundChannel.attr(ONGOINGSTREAMS_KEY).get().get(streamId) == channel)
                            boundChannel.writeAndFlush(new Message()
                                    .setType(MessageType.Data)
                                    .setStreamId(streamId)
                                    .setPayload(msg.content().retainedDuplicate()));
                        else
                            channel.close();
                    }

                    @Override
                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception
                    {
                        if (evt instanceof IdleStateEvent && ((IdleStateEvent) evt).state() == IdleState.ALL_IDLE)
                        {
                            final Channel channel = ctx.channel();

                            channel.close();
                        }
                    }

                });
            }

        }).connect(toOpen);
    }

    @Override
    public String toString()
    {
        return identifier;
    }

}

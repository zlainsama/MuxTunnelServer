package me.lain.muxtun.mipo;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOption;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.Shared;
import me.lain.muxtun.codec.Message;
import me.lain.muxtun.codec.Message.MessageType;
import me.lain.muxtun.codec.SnappyCodec;

@Sharable
class LinkInboundHandler extends ChannelInboundHandlerAdapter
{

    static final LinkInboundHandler DEFAULT = new LinkInboundHandler();

    private static ChannelFuture open(SocketAddress toOpen, Function<Channel, PayloadWriter> writerBuilder)
    {
        return new Bootstrap()
                .group(Shared.NettyObjects.workerGroup)
                .channel(Shared.NettyObjects.classSocketChannel)
                .handler(TCPStreamInitializer.DEFAULT)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .option(ChannelOption.TCP_NODELAY, true)
                .connect(toOpen)
                .addListener(new ChannelFutureListener()
                {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception
                    {
                        if (future.isSuccess())
                            future.channel().attr(Vars.WRITER_KEY).set(writerBuilder.apply(future.channel()));
                    }

                });
    }

    private static ChannelFuture openUDP(SocketAddress toOpen, Function<Channel, PayloadWriter> writerBuilder)
    {
        return new Bootstrap()
                .group(Shared.NettyObjects.workerGroup)
                .channel(Shared.NettyObjects.classDatagramChannel)
                .handler(UDPStreamInitializer.DEFAULT)
                .connect(toOpen)
                .addListener(new ChannelFutureListener()
                {

                    @Override
                    public void operationComplete(ChannelFuture future) throws Exception
                    {
                        if (future.isSuccess())
                            future.channel().attr(Vars.WRITER_KEY).set(writerBuilder.apply(future.channel()));
                    }

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

    private void handleMessage(ChannelHandlerContext ctx, Message msg) throws Exception
    {
        if (ctx.channel().isActive())
        {
            LinkSession session = ctx.channel().attr(Vars.SESSION_KEY).get();

            switch (msg.type())
            {
                case PING:
                {
                    if (session.authStatus.completed)
                    {
                        ctx.writeAndFlush(MessageType.PING.create());
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case OPEN:
                {
                    if (session.authStatus.completed)
                    {
                        UUID requestId = msg.getStreamId();

                        Optional<SocketAddress> toOpen = session.targetTableLookup.apply(requestId);
                        if (toOpen.isPresent())
                        {
                            open(toOpen.get(), stream -> {
                                UUID streamId = UUID.randomUUID();
                                if (ctx.channel().isActive())
                                {
                                    session.ongoingStreams.put(streamId, stream);
                                    ctx.writeAndFlush(MessageType.OPEN.create().setStreamId(streamId));
                                    stream.closeFuture().addListener(future -> {
                                        if (ctx.channel().isActive() && session.ongoingStreams.remove(streamId) == stream)
                                            ctx.writeAndFlush(MessageType.DROP.create().setStreamId(streamId));
                                    });
                                }
                                else
                                {
                                    stream.close();
                                }
                                return payload -> {
                                    try
                                    {
                                        if (!ctx.channel().isActive())
                                            return false;
                                        ctx.writeAndFlush(MessageType.DATA.create().setStreamId(streamId).setPayload(payload.retain()));
                                        return true;
                                    }
                                    finally
                                    {
                                        ReferenceCountUtil.release(payload);
                                    }
                                };
                            }).addListener(future -> {
                                if (!future.isSuccess())
                                    ctx.writeAndFlush(MessageType.DROP.create().setStreamId(requestId));
                            }).addListener(session.channelAccumulator);
                        }
                        else
                        {
                            ctx.writeAndFlush(MessageType.DROP.create().setStreamId(requestId)).addListener(ChannelFutureListener.CLOSE);
                        }
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case DATA:
                {
                    if (session.authStatus.completed)
                    {
                        UUID streamId = msg.getStreamId();
                        ByteBuf payload = msg.getPayload();

                        Channel toSend = session.ongoingStreams.get(streamId);
                        if (toSend != null && toSend.isActive())
                            toSend.writeAndFlush(payload.retain());
                        else
                            ctx.writeAndFlush(MessageType.DROP.create().setStreamId(streamId));
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case DROP:
                {
                    if (session.authStatus.completed)
                    {
                        UUID streamId = msg.getStreamId();

                        Channel toClose = session.ongoingStreams.remove(streamId);
                        if (toClose != null && toClose.isActive())
                            toClose.close();
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case OPENUDP:
                {
                    if (session.authStatus.completed)
                    {
                        UUID requestId = msg.getStreamId();

                        Optional<SocketAddress> toOpen = session.targetTableLookup.apply(requestId);
                        if (toOpen.isPresent())
                        {
                            openUDP(toOpen.get(), stream -> {
                                UUID streamId = UUID.randomUUID();
                                if (ctx.channel().isActive())
                                {
                                    session.ongoingStreams.put(streamId, stream);
                                    ctx.writeAndFlush(MessageType.OPENUDP.create().setStreamId(streamId));
                                    stream.closeFuture().addListener(future -> {
                                        if (ctx.channel().isActive() && session.ongoingStreams.remove(streamId) == stream)
                                            ctx.writeAndFlush(MessageType.DROP.create().setStreamId(streamId));
                                    });
                                }
                                else
                                {
                                    stream.close();
                                }
                                return payload -> {
                                    try
                                    {
                                        if (!ctx.channel().isActive())
                                            return false;
                                        ctx.writeAndFlush(MessageType.DATA.create().setStreamId(streamId).setPayload(payload.retain()));
                                        return true;
                                    }
                                    finally
                                    {
                                        ReferenceCountUtil.release(payload);
                                    }
                                };
                            }).addListener(future -> {
                                if (!future.isSuccess())
                                    ctx.writeAndFlush(MessageType.DROP.create().setStreamId(requestId));
                            }).addListener(session.channelAccumulator);
                        }
                        else
                        {
                            ctx.writeAndFlush(MessageType.DROP.create().setStreamId(requestId)).addListener(ChannelFutureListener.CLOSE);
                        }
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case AUTH:
                {
                    if (session.authStatus.initiated && session.authStatus.challenge != null)
                    {
                        byte[] challenge = session.authStatus.challenge;
                        session.authStatus.challenge = null;
                        ByteBuf payload = msg.getPayload();
                        byte[] answer = ByteBufUtil.getBytes(payload, payload.readerIndex(), payload.readableBytes(), false);

                        if (Arrays.equals(challenge, answer))
                        {
                            session.authStatus.completed = true;
                            ctx.writeAndFlush(MessageType.AUTH.create().setPayload(Unpooled.EMPTY_BUFFER));
                        }
                        else
                        {
                            session.authStatus.completed = false;
                            ctx.close();
                        }
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case AUTHREQ:
                {
                    if (!session.authStatus.initiated && session.authStatus.challenge == null)
                    {
                        session.authStatus.initiated = true;
                        ByteBuf buf = null;

                        try
                        {
                            UUID id = UUID.randomUUID();
                            buf = Unpooled.buffer(16, 16).writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits());
                            byte[] question = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);

                            Optional<byte[]> challenge = session.challengeGenerator.apply(question);
                            if (challenge.isPresent())
                            {
                                session.authStatus.challenge = challenge.get();
                                ctx.writeAndFlush(MessageType.AUTHREQ.create().setPayload(buf.retain()));
                            }
                            else
                            {
                                Optional<byte[]> challenge_3 = session.challengeGenerator_3.apply(question);
                                if (challenge_3.isPresent())
                                {
                                    session.authStatus.challenge = challenge_3.get();
                                    ctx.writeAndFlush(MessageType.AUTHREQ3.create().setPayload(buf.retain()));
                                }
                                else
                                {
                                    ctx.close();
                                }
                            }
                        }
                        finally
                        {
                            ReferenceCountUtil.release(buf);
                        }
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case AUTHREQ3:
                {
                    if (!session.authStatus.initiated && session.authStatus.challenge == null)
                    {
                        session.authStatus.initiated = true;
                        ByteBuf buf = null;

                        try
                        {
                            UUID id = UUID.randomUUID();
                            buf = Unpooled.buffer(16, 16).writeLong(id.getMostSignificantBits()).writeLong(id.getLeastSignificantBits());
                            byte[] question = ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false);

                            Optional<byte[]> challenge_3 = session.challengeGenerator_3.apply(question);
                            if (challenge_3.isPresent())
                            {
                                session.authStatus.challenge = challenge_3.get();
                                ctx.writeAndFlush(MessageType.AUTHREQ3.create().setPayload(buf.retain()));
                            }
                            else
                            {
                                Optional<byte[]> challenge = session.challengeGenerator.apply(question);
                                if (challenge.isPresent())
                                {
                                    session.authStatus.challenge = challenge.get();
                                    ctx.writeAndFlush(MessageType.AUTHREQ.create().setPayload(buf.retain()));
                                }
                                else
                                {
                                    ctx.close();
                                }
                            }
                        }
                        finally
                        {
                            ReferenceCountUtil.release(buf);
                        }
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                case SNAPPY:
                {
                    if (session.authStatus.completed)
                    {
                        ctx.pipeline().addBefore("FrameCodec", "SnappyCodec", new SnappyCodec());
                    }
                    else
                    {
                        ctx.close();
                    }
                    break;
                }
                default:
                {
                    ctx.close();
                    break;
                }
            }
        }
    }

}

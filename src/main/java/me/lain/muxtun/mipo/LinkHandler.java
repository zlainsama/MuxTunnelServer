package me.lain.muxtun.mipo;

import java.util.Arrays;
import java.util.UUID;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.codec.Message;
import me.lain.muxtun.codec.Message.MessageType;

@Sharable
class LinkHandler extends ChannelDuplexHandler
{

    static final LinkHandler DEFAULT = new LinkHandler();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof Message)
        {
            Message cast = (Message) msg;

            try
            {
                handleMessage(LinkContext.getContext(ctx.channel()), cast);
            }
            finally
            {
                ReferenceCountUtil.release(cast);
            }
        }
        else
        {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        Vars.ChannelError.accumulate(ctx.channel(), cause);

        ctx.close();
    }

    private void handleMessage(LinkContext lctx, Message msg) throws Exception
    {
        if (lctx.isActive())
        {
            switch (msg.type())
            {
                case PING:
                {
                    lctx.writeAndFlush(MessageType.PING.create());
                    break;
                }
                case JOINSESSION:
                {
                    if (lctx.getSession() == null)
                    {
                        UUID id = msg.getId();
                        ByteBuf buf = msg.getBuf();
                        byte[] challenge = buf != null ? ByteBufUtil.getBytes(buf.readSlice(buf.readableBytes())) : null;

                        if (challenge != null && challenge.length <= 4096)
                        {
                            boolean[] created = new boolean[] { false };
                            if (lctx.getManager().getSessions().compute(id, (key, value) -> {
                                if (value != null)
                                {
                                    if (Arrays.equals(challenge, value.getChallenge()) && value.join(lctx.getChannel()))
                                    {
                                        lctx.setSession(value);
                                    }
                                }
                                else
                                {
                                    created[0] = true;

                                    if ((value = new LinkSession(key, lctx.getManager(), Vars.SESSIONS.next(), challenge)).join(lctx.getChannel()))
                                    {
                                        lctx.setSession(value);
                                    }
                                    else
                                    {
                                        value.scheduledSelfClose(true);
                                    }
                                }

                                return value;
                            }) == lctx.getSession())
                            {
                                lctx.writeAndFlush(MessageType.JOINSESSION.create().setId(id).setBuf(lctx.getChannel().alloc().buffer(1).writeBoolean(created[0])));
                            }
                            else
                            {
                                lctx.writeAndFlush(MessageType.JOINSESSION.create());
                            }
                        }
                        else
                        {
                            lctx.close();
                        }
                    }
                    else
                    {
                        lctx.close();
                    }
                    break;
                }
                case OPENSTREAM:
                {
                    if (lctx.getSession() != null)
                    {
                        LinkSession session = lctx.getSession();
                        int seq = msg.getSeq();

                        if (session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(session.updateReceived()));
                    }
                    else
                    {
                        lctx.close();
                    }
                    break;
                }
                case OPENSTREAMUDP:
                {
                    if (lctx.getSession() != null)
                    {
                        LinkSession session = lctx.getSession();
                        int seq = msg.getSeq();

                        if (session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(session.updateReceived()));
                    }
                    else
                    {
                        lctx.close();
                    }
                    break;
                }
                case CLOSESTREAM:
                {
                    if (lctx.getSession() != null)
                    {
                        LinkSession session = lctx.getSession();
                        int seq = msg.getSeq();

                        if (session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(session.updateReceived()));
                    }
                    else
                    {
                        lctx.close();
                    }
                    break;
                }
                case DATASTREAM:
                {
                    if (lctx.getSession() != null)
                    {
                        LinkSession session = lctx.getSession();
                        int seq = msg.getSeq();

                        if (session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(session.updateReceived()));
                    }
                    else
                    {
                        lctx.close();
                    }
                    break;
                }
                case ACKNOWLEDGE:
                {
                    if (lctx.getSession() != null)
                    {
                        lctx.getRTTM().complete().ifPresent(lctx.getSRTT()::updateAndGet);
                        LinkSession session = lctx.getSession();
                        int ack = msg.getAck();

                        session.acknowledge(ack);
                    }
                    else
                    {
                        lctx.close();
                    }
                    break;
                }
                default:
                {
                    lctx.close();
                    break;
                }
            }
        }
    }

    private void onMessageWrite(LinkContext lctx, Message msg, ChannelPromise promise) throws Exception
    {
        switch (msg.type())
        {
            case OPENSTREAM:
            case OPENSTREAMUDP:
            case CLOSESTREAM:
            case DATASTREAM:
                promise.addListener(future -> {
                    if (future.isSuccess())
                        lctx.getRTTM().initiate();
                });
                break;
            default:
                break;
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
        if (msg instanceof Message)
            onMessageWrite(LinkContext.getContext(ctx.channel()), (Message) msg, promise);

        ctx.write(msg, promise);
    }

}

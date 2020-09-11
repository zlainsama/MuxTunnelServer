package me.lain.muxtun.mipo;

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFutureListener;
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
                    lctx.getSRTT().reset();
                    break;
                }
                case JOINSESSION:
                {
                    if (lctx.getSession() == null)
                    {
                        UUID id = msg.getId();
                        UUID id2 = msg.getId2();
                        ByteBuf buf = msg.getBuf();
                        byte[] challenge = buf != null ? ByteBufUtil.getBytes(buf, buf.readerIndex(), buf.readableBytes(), false) : null;

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
                                    Optional<SocketAddress> address = lctx.getManager().getResources().getTargetTableLookup().apply(id2);
                                    if (address.isPresent())
                                    {
                                        created[0] = true;

                                        if ((value = new LinkSession(key, lctx.getManager(), Vars.SESSIONS.next(), challenge.clone(), address.get())).join(lctx.getChannel()))
                                        {
                                            lctx.setSession(value);
                                        }
                                    }
                                }

                                return value;
                            }) == lctx.getSession())
                            {
                                if (created[0])
                                    lctx.writeAndFlush(MessageType.JOINSESSION.create().setId(id).setBuf(Vars.TRUE_BUFFER.duplicate()));
                                else
                                    lctx.writeAndFlush(MessageType.JOINSESSION.create().setId(id).setBuf(Vars.FALSE_BUFFER.duplicate()));
                            }
                            else
                            {
                                lctx.writeAndFlush(MessageType.JOINSESSION.create()).addListener(ChannelFutureListener.CLOSE);
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

                        boolean inRange;
                        if (inRange = session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        session.updateReceived(ack -> lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(ack).setSAck(inRange ? seq : ack - 1)));
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

                        boolean inRange;
                        if (inRange = session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        session.updateReceived(ack -> lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(ack).setSAck(inRange ? seq : ack - 1)));
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

                        boolean inRange;
                        if (inRange = session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        session.updateReceived(ack -> lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(ack).setSAck(inRange ? seq : ack - 1)));
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

                        boolean inRange;
                        if (inRange = session.getFlowControl().inRange(seq))
                            session.getInboundBuffer().computeIfAbsent(seq, key -> ReferenceCountUtil.retain(msg));
                        session.updateReceived(ack -> lctx.writeAndFlush(MessageType.ACKNOWLEDGE.create().setAck(ack).setSAck(inRange ? seq : ack - 1)));
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
                        int sack = msg.getSAck();

                        session.acknowledge(ack, sack);
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

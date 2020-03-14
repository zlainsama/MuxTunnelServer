package me.lain.muxtun.codec;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.codec.Message.MessageType;

@ChannelHandler.Sharable
public class MessageEncoder extends ChannelOutboundHandlerAdapter
{

    public static final MessageEncoder DEFAULT = new MessageEncoder();

    protected Object encode(ChannelHandlerContext ctx, Message msg)
    {
        switch (msg.getType())
        {
            case Ping:
                return ctx.alloc().buffer(1)
                        .writeByte(MessageType.Ping.getId());
            case Open:
                return ctx.alloc().buffer(17)
                        .writeByte(MessageType.Open.getId())
                        .writeLong(msg.getStreamId().getMostSignificantBits()).writeLong(msg.getStreamId().getLeastSignificantBits());
            case Data:
                return ctx.alloc().buffer(17 + msg.getPayload().readableBytes())
                        .writeByte(MessageType.Data.getId())
                        .writeLong(msg.getStreamId().getMostSignificantBits()).writeLong(msg.getStreamId().getLeastSignificantBits())
                        .writeBytes(msg.getPayload());
            case Drop:
                return ctx.alloc().buffer(17)
                        .writeByte(MessageType.Drop.getId())
                        .writeLong(msg.getStreamId().getMostSignificantBits()).writeLong(msg.getStreamId().getLeastSignificantBits());
            case OpenUDP:
                return ctx.alloc().buffer(17)
                        .writeByte(MessageType.OpenUDP.getId())
                        .writeLong(msg.getStreamId().getMostSignificantBits()).writeLong(msg.getStreamId().getLeastSignificantBits());
            case Auth:
                return ctx.alloc().buffer(1 + msg.getPayload().readableBytes())
                        .writeByte(MessageType.Auth.getId())
                        .writeBytes(msg.getPayload());
            case AuthReq:
                return ctx.alloc().buffer(1 + msg.getPayload().readableBytes())
                        .writeByte(MessageType.AuthReq.getId())
                        .writeBytes(msg.getPayload());
            case AuthReq_3:
                return ctx.alloc().buffer(1 + msg.getPayload().readableBytes())
                        .writeByte(MessageType.AuthReq_3.getId())
                        .writeBytes(msg.getPayload());
            default:
                throw new IllegalArgumentException("UnknownMessageType");
        }
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
        if (msg instanceof Message)
        {
            Message cast = (Message) msg;

            try
            {
                Object result = encode(ctx, cast);
                if (result != null)
                    ctx.write(result, promise);
                else
                    throw new Error("BadEncoder");
            }
            finally
            {
                ReferenceCountUtil.release(cast.getPayload());
            }
        }
        else
        {
            ctx.write(msg, promise);
        }
    }

}

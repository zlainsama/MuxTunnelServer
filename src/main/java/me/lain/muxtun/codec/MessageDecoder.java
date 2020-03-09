package me.lain.muxtun.codec;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.codec.Message.MessageType;

@ChannelHandler.Sharable
public class MessageDecoder extends ChannelInboundHandlerAdapter
{

    public static final MessageDecoder DEFAULT = new MessageDecoder();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof ByteBuf)
        {
            ByteBuf cast = (ByteBuf) msg;

            try
            {
                Object result = decode(ctx, cast);
                if (result != null)
                    ctx.fireChannelRead(result);
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

    protected Object decode(ChannelHandlerContext ctx, ByteBuf msg) throws Exception
    {
        switch (MessageType.find(msg.readByte()))
        {
            case Ping:
                return new Message()
                        .setType(MessageType.Ping);
            case Open:
                return new Message()
                        .setType(MessageType.Open)
                        .setStreamId(new UUID(msg.readLong(), msg.readLong()));
            case Data:
                return new Message()
                        .setType(MessageType.Data)
                        .setStreamId(new UUID(msg.readLong(), msg.readLong()))
                        .setPayload(msg.readableBytes() > 0 ? msg.readRetainedSlice(msg.readableBytes()) : Unpooled.EMPTY_BUFFER);
            case Drop:
                return new Message()
                        .setType(MessageType.Drop)
                        .setStreamId(new UUID(msg.readLong(), msg.readLong()));
            case OpenUDP:
                return new Message()
                        .setType(MessageType.OpenUDP)
                        .setStreamId(new UUID(msg.readLong(), msg.readLong()));
            case Auth:
                return new Message()
                        .setType(MessageType.Auth)
                        .setPayload(msg.readableBytes() > 0 ? msg.readRetainedSlice(msg.readableBytes()) : Unpooled.EMPTY_BUFFER);
            case AuthReq:
                return new Message()
                        .setType(MessageType.AuthReq)
                        .setPayload(msg.readableBytes() > 0 ? msg.readRetainedSlice(msg.readableBytes()) : Unpooled.EMPTY_BUFFER);
            case AuthReq_3:
                return new Message()
                        .setType(MessageType.AuthReq_3)
                        .setPayload(msg.readableBytes() > 0 ? msg.readRetainedSlice(msg.readableBytes()) : Unpooled.EMPTY_BUFFER);
            default:
                throw new CorruptedFrameException("UnknownMessageType");
        }
    }

}

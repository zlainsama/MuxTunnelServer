package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.util.ReferenceCountUtil;

@ChannelHandler.Sharable
public class MessageCodec extends ChannelDuplexHandler
{

    public static final MessageCodec DEFAULT = new MessageCodec();

    private final MessageDecoder decoder;
    private final MessageEncoder encoder;

    public MessageCodec()
    {
        this(MessageDecoder.DEFAULT, MessageEncoder.DEFAULT);
    }

    public MessageCodec(MessageDecoder decoder, MessageEncoder encoder)
    {
        if (decoder == null || encoder == null)
            throw new NullPointerException();
        if (!decoder.isSharable() || !encoder.isSharable())
            throw new IllegalArgumentException();

        this.decoder = decoder;
        this.encoder = encoder;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof ByteBuf)
        {
            ByteBuf cast = (ByteBuf) msg;

            try
            {
                Object result = decoder.decode(ctx, cast);
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

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception
    {
        if (msg instanceof Message)
        {
            Message cast = (Message) msg;

            try
            {
                Object result = encoder.encode(ctx, cast);
                if (result != null)
                    ctx.write(result, promise);
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

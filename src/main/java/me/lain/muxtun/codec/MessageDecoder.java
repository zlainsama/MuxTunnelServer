package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.util.ReferenceCountUtil;
import me.lain.muxtun.codec.Message.MessageType;

public class MessageDecoder extends LengthFieldBasedFrameDecoder {

    public MessageDecoder() {
        super(1048576, 0, 3, 0, 3);
        setCumulator(COMPOSITE_CUMULATOR);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception {
        Object obj = super.decode(ctx, in);

        if (obj instanceof ByteBuf) {
            ByteBuf buf = (ByteBuf) obj;
            Message msg = null;
            boolean release = true;

            try {
                msg = MessageType.find(buf.readByte()).create();
                msg.decode(buf);
                release = false;
                return msg;
            } finally {
                if (release && msg != null)
                    ReferenceCountUtil.release(msg);
                ReferenceCountUtil.release(buf);
            }
        } else {
            return obj;
        }
    }

}

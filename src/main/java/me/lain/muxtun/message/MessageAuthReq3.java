package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.util.ReferenceCounted;
import me.lain.muxtun.codec.Message;

public class MessageAuthReq3 implements Message, ReferenceCounted
{

    public static MessageAuthReq3 create()
    {
        return new MessageAuthReq3();
    }

    private ByteBuf payload;

    private MessageAuthReq3()
    {
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        payload = buf.readableBytes() > 0 ? buf.readRetainedSlice(buf.readableBytes()) : Unpooled.EMPTY_BUFFER;
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        buf.writeBytes(payload != null ? payload : Unpooled.EMPTY_BUFFER);
    }

    @Override
    public ByteBuf getPayload()
    {
        return payload;
    }

    @Override
    public int refCnt()
    {
        if (payload != null)
            return payload.refCnt();
        return 1;
    }

    @Override
    public boolean release()
    {
        if (payload != null)
            return payload.release();
        return false;
    }

    @Override
    public boolean release(int decrement)
    {
        if (payload != null)
            return payload.release(decrement);
        return false;
    }

    @Override
    public ReferenceCounted retain()
    {
        if (payload != null)
            payload.retain();
        return this;
    }

    @Override
    public ReferenceCounted retain(int increment)
    {
        if (payload != null)
            payload.retain(increment);
        return this;
    }

    @Override
    public MessageAuthReq3 setPayload(ByteBuf payload)
    {
        this.payload = payload;
        return this;
    }

    @Override
    public ReferenceCounted touch()
    {
        if (payload != null)
            payload.touch();
        return this;
    }

    @Override
    public ReferenceCounted touch(Object hint)
    {
        if (payload != null)
            payload.touch(hint);
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.AUTHREQ3;
    }

}

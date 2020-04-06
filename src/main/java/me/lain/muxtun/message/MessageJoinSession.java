package me.lain.muxtun.message;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import me.lain.muxtun.codec.Message;

public class MessageJoinSession implements Message, ReferenceCounted
{

    public static MessageJoinSession create()
    {
        return new MessageJoinSession();
    }

    private UUID id;
    private ByteBuf buf;

    private MessageJoinSession()
    {
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        if (buf.readableBytes() < 16)
        {
            setId(null);
            setBuf(null);
        }
        else
        {
            setId(new UUID(buf.readLong(), buf.readLong()));
            setBuf(buf.readableBytes() > 0 ? buf.readRetainedSlice(buf.readableBytes()) : null);
        }
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        if (getId() != null)
        {
            buf.writeLong(getId().getMostSignificantBits()).writeLong(getId().getLeastSignificantBits());

            if (getBuf() != null)
                buf.writeBytes(getBuf());
        }
    }

    @Override
    public ByteBuf getBuf()
    {
        return buf;
    }

    @Override
    public UUID getId()
    {
        return id;
    }

    @Override
    public int refCnt()
    {
        if (buf != null)
            return buf.refCnt();
        return 0;
    }

    @Override
    public boolean release()
    {
        if (buf != null)
            return buf.release();
        return false;
    }

    @Override
    public boolean release(int decrement)
    {
        if (buf != null)
            return buf.release(decrement);
        return false;
    }

    @Override
    public MessageJoinSession retain()
    {
        if (buf != null)
            buf.retain();
        return this;
    }

    @Override
    public MessageJoinSession retain(int increment)
    {
        if (buf != null)
            buf.retain(increment);
        return this;
    }

    @Override
    public MessageJoinSession setBuf(ByteBuf buf)
    {
        this.buf = buf;
        return this;
    }

    @Override
    public MessageJoinSession setId(UUID id)
    {
        this.id = id;
        return this;
    }

    @Override
    public MessageJoinSession touch()
    {
        if (buf != null)
            buf.touch();
        return this;
    }

    @Override
    public MessageJoinSession touch(Object hint)
    {
        if (buf != null)
            buf.touch(hint);
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.JOINSESSION;
    }

}

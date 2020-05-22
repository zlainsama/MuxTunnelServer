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
    private UUID id2;
    private ByteBuf buf;

    private MessageJoinSession()
    {
    }

    @Override
    public Message copy()
    {
        return type().create().setId(getId()).setId2(getId2()).setBuf(Vars.retainedDuplicate(getBuf()));
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        if (buf.readableBytes() < 16)
        {
            setId(null);
            setId2(null);
            setBuf(null);
        }
        else if (buf.readableBytes() < 32)
        {
            setId(new UUID(buf.readLong(), buf.readLong()));
            setId2(null);
            setBuf(buf.readableBytes() > 0 ? buf.readBytes(buf.readableBytes()) : null);
        }
        else
        {
            setId(new UUID(buf.readLong(), buf.readLong()));
            setId2(new UUID(buf.readLong(), buf.readLong()));
            setBuf(buf.readableBytes() > 0 ? buf.readBytes(buf.readableBytes()) : null);
        }
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        UUID _id = getId();
        if (_id != null)
        {
            buf.writeLong(_id.getMostSignificantBits()).writeLong(_id.getLeastSignificantBits());

            UUID _id2 = getId2();
            if (_id2 != null)
                buf.writeLong(_id2.getMostSignificantBits()).writeLong(_id2.getLeastSignificantBits());

            ByteBuf _buf = getBuf();
            if (_buf != null)
                buf.writeBytes(_buf);
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
    public UUID getId2()
    {
        return id2;
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
    public MessageJoinSession setId2(UUID id2)
    {
        this.id2 = id2;
        return this;
    }

    @Override
    public int size()
    {
        return getId() != null ? 16 + (getId2() != null ? 16 : 0) + Vars.getSize(getBuf()) : 0;
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

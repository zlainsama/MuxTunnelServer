package me.lain.muxtun.message;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageCloseStream implements Message
{

    public static MessageCloseStream create()
    {
        return new MessageCloseStream();
    }

    private int seq;
    private UUID id;

    private MessageCloseStream()
    {
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        setSeq(buf.readInt());
        setId(new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        buf.writeInt(getSeq());
        buf.writeLong(getId().getMostSignificantBits()).writeLong(getId().getLeastSignificantBits());
    }

    @Override
    public UUID getId()
    {
        return id;
    }

    @Override
    public int getSeq()
    {
        return seq;
    }

    @Override
    public MessageCloseStream setId(UUID id)
    {
        this.id = id;
        return this;
    }

    @Override
    public MessageCloseStream setSeq(int seq)
    {
        this.seq = seq;
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.CLOSESTREAM;
    }

}
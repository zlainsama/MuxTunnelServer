package me.lain.muxtun.message;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageOpenStream implements Message
{

    public static MessageOpenStream create()
    {
        return new MessageOpenStream();
    }

    private int seq;
    private UUID id;

    private MessageOpenStream()
    {
    }

    @Override
    public Message copy()
    {
        return type().create().setSeq(getSeq()).setId(getId());
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        setSeq(buf.readInt());
        setId(buf.readableBytes() == 16 ? new UUID(buf.readLong(), buf.readLong()) : null);
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        int _seq = getSeq();
        buf.writeInt(_seq);

        UUID _id = getId();
        if (_id != null)
            buf.writeLong(_id.getMostSignificantBits()).writeLong(_id.getLeastSignificantBits());
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
    public MessageOpenStream setId(UUID id)
    {
        this.id = id;
        return this;
    }

    @Override
    public MessageOpenStream setSeq(int seq)
    {
        this.seq = seq;
        return this;
    }

    @Override
    public int size()
    {
        return getId() != null ? 20 : 4;
    }

    @Override
    public MessageType type()
    {
        return MessageType.OPENSTREAM;
    }

}

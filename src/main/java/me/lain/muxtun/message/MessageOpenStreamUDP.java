package me.lain.muxtun.message;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageOpenStreamUDP implements Message
{

    public static MessageOpenStreamUDP create()
    {
        return new MessageOpenStreamUDP();
    }

    private int seq;
    private UUID id;

    private MessageOpenStreamUDP()
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
        buf.writeInt(getSeq());

        if (getId() != null)
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
    public MessageOpenStreamUDP setId(UUID id)
    {
        this.id = id;
        return this;
    }

    @Override
    public MessageOpenStreamUDP setSeq(int seq)
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
        return MessageType.OPENSTREAMUDP;
    }

}

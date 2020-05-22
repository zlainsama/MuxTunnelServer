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
    private int req;
    private UUID id;

    private MessageCloseStream()
    {
    }

    @Override
    public Message copy()
    {
        return type().create().setSeq(getSeq()).setReq(getReq()).setId(getId());
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        setSeq(buf.readInt());
        setReq(buf.readInt());
        setId(new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        int _seq = getSeq();
        buf.writeInt(_seq);

        int _req = getReq();
        buf.writeInt(_req);

        UUID _id = getId();
        buf.writeLong(_id.getMostSignificantBits()).writeLong(_id.getLeastSignificantBits());
    }

    @Override
    public UUID getId()
    {
        return id;
    }

    @Override
    public int getReq()
    {
        return req;
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
    public MessageCloseStream setReq(int req)
    {
        this.req = req;
        return this;
    }

    @Override
    public MessageCloseStream setSeq(int seq)
    {
        this.seq = seq;
        return this;
    }

    @Override
    public int size()
    {
        return 24;
    }

    @Override
    public MessageType type()
    {
        return MessageType.CLOSESTREAM;
    }

}

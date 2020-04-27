package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageAcknowledge implements Message
{

    public static MessageAcknowledge create()
    {
        return new MessageAcknowledge();
    }

    private int ack;
    private int sack;

    private MessageAcknowledge()
    {
    }

    @Override
    public Message copy()
    {
        return type().create().setAck(getAck()).setSAck(getSAck());
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        if (buf.readableBytes() > 4)
        {
            setAck(buf.readInt());
            setSAck(buf.readInt());
        }
        else
        {
            setAck(buf.readInt());
            setSAck(getAck() - 1);
        }
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        if (getAck() < getSAck())
        {
            buf.writeInt(getAck());
            buf.writeInt(getSAck());
        }
        else
        {
            buf.writeInt(getAck());
        }
    }

    @Override
    public int getAck()
    {
        return ack;
    }

    @Override
    public int getSAck()
    {
        return sack;
    }

    @Override
    public MessageAcknowledge setAck(int ack)
    {
        this.ack = ack;
        return this;
    }

    @Override
    public MessageAcknowledge setSAck(int sack)
    {
        this.sack = sack;
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.ACKNOWLEDGE;
    }

}

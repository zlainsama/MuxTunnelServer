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

    private MessageAcknowledge()
    {
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        setAck(buf.readInt());
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        buf.writeInt(getAck());
    }

    @Override
    public int getAck()
    {
        return ack;
    }

    @Override
    public MessageAcknowledge setAck(int ack)
    {
        this.ack = ack;
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.ACKNOWLEDGE;
    }

}

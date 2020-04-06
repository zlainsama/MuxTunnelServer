package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessagePing implements Message
{

    private static MessagePing INSTANCE = new MessagePing();

    public static MessagePing create()
    {
        return INSTANCE;
    }

    private MessagePing()
    {
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
    }

    @Override
    public MessageType type()
    {
        return MessageType.PING;
    }

}

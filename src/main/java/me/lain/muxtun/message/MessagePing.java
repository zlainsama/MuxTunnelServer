package me.lain.muxtun.message;

import me.lain.muxtun.codec.Message;

public class MessagePing implements Message
{

    private static final MessagePing INSTANCE = new MessagePing();

    public static MessagePing create()
    {
        return INSTANCE;
    }

    private MessagePing()
    {
    }

    @Override
    public MessageType type()
    {
        return MessageType.PING;
    }

}

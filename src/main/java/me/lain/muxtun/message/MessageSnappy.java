package me.lain.muxtun.message;

import me.lain.muxtun.codec.Message;

public class MessageSnappy implements Message
{

    private static final MessageSnappy INSTANCE = new MessageSnappy();

    public static MessageSnappy create()
    {
        return INSTANCE;
    }

    private MessageSnappy()
    {
    }

    @Override
    public MessageType type()
    {
        return MessageType.SNAPPY;
    }

}

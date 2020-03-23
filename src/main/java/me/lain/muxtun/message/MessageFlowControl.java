package me.lain.muxtun.message;

import me.lain.muxtun.codec.Message;

public class MessageFlowControl implements Message
{

    private static final MessageFlowControl INSTANCE = new MessageFlowControl();

    public static MessageFlowControl create()
    {
        return INSTANCE;
    }

    private MessageFlowControl()
    {
    }

    @Override
    public MessageType type()
    {
        return MessageType.FLOWCONTROL;
    }

}

package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessagePing implements Message {

    private static final MessagePing INSTANCE = new MessagePing();

    private MessagePing() {
    }

    public static MessagePing create() {
        return INSTANCE;
    }

    @Override
    public Message copy() {
        return type().create();
    }

    @Override
    public void decode(ByteBuf buf) throws Exception {
    }

    @Override
    public void encode(ByteBuf buf) throws Exception {
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public MessageType type() {
        return MessageType.PING;
    }

}

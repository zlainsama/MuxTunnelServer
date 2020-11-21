package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageLinkConfig implements Message {

    private short priority;
    private long writeLimit;

    private MessageLinkConfig() {
    }

    public static MessageLinkConfig create() {
        return new MessageLinkConfig();
    }

    @Override
    public Message copy() {
        return type().create().setPriority(getPriority()).setWriteLimit(getWriteLimit());
    }

    @Override
    public void decode(ByteBuf buf) throws Exception {
        if (buf.readableBytes() > 2) {
            setPriority(buf.readShort());
            setWriteLimit(buf.readLong());
        } else {
            setPriority(buf.readShort());
            setWriteLimit(0L);
        }
    }

    @Override
    public void encode(ByteBuf buf) throws Exception {
        if (getWriteLimit() > 0L) {
            short _priority = getPriority();
            buf.writeShort(_priority);

            long _writeLimit = getWriteLimit();
            buf.writeLong(_writeLimit);
        } else {
            short _priority = getPriority();
            buf.writeShort(_priority);
        }
    }

    @Override
    public short getPriority() {
        return priority;
    }

    @Override
    public MessageLinkConfig setPriority(short priority) {
        this.priority = priority;
        return this;
    }

    @Override
    public long getWriteLimit() {
        return writeLimit;
    }

    @Override
    public MessageLinkConfig setWriteLimit(long writeLimit) {
        this.writeLimit = writeLimit;
        return this;
    }

    @Override
    public int size() {
        return getWriteLimit() > 0L ? 10 : 2;
    }

    @Override
    public MessageType type() {
        return MessageType.LINKCONFIG;
    }

}

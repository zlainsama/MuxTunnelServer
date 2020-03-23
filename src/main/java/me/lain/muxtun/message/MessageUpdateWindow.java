package me.lain.muxtun.message;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageUpdateWindow implements Message
{

    public static MessageUpdateWindow create()
    {
        return new MessageUpdateWindow();
    }

    private UUID streamId;
    private int increment;

    private MessageUpdateWindow()
    {
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        streamId = new UUID(buf.readLong(), buf.readLong());
        increment = buf.readInt();
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        buf.writeLong(streamId.getMostSignificantBits()).writeLong(streamId.getLeastSignificantBits());
        buf.writeInt(increment);
    }

    @Override
    public UUID getStreamId()
    {
        return streamId;
    }

    @Override
    public int getWindowSizeIncrement()
    {
        return increment;
    }

    @Override
    public MessageUpdateWindow setStreamId(UUID streamId)
    {
        this.streamId = streamId;
        return this;
    }

    @Override
    public MessageUpdateWindow setWindowSizeIncrement(int increment)
    {
        this.increment = increment;
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.UPDATEWINDOW;
    }

}

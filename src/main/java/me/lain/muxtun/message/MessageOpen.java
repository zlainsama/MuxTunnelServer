package me.lain.muxtun.message;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageOpen implements Message
{

    public static MessageOpen create()
    {
        return new MessageOpen();
    }

    private UUID streamId;

    private MessageOpen()
    {
    }

    @Override
    public void decode(ByteBuf buf) throws Exception
    {
        streamId = new UUID(buf.readLong(), buf.readLong());
    }

    @Override
    public void encode(ByteBuf buf) throws Exception
    {
        buf.writeLong(streamId.getMostSignificantBits()).writeLong(streamId.getLeastSignificantBits());
    }

    @Override
    public UUID getStreamId()
    {
        return streamId;
    }

    @Override
    public MessageOpen setStreamId(UUID streamId)
    {
        this.streamId = streamId;
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.OPEN;
    }

}

package me.lain.muxtun.message;

import java.util.UUID;
import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageDrop implements Message
{

    public static MessageDrop create()
    {
        return new MessageDrop();
    }

    private UUID streamId;

    private MessageDrop()
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
    public MessageDrop setStreamId(UUID requestId)
    {
        this.streamId = requestId;
        return this;
    }

    @Override
    public MessageType type()
    {
        return MessageType.DROP;
    }

}

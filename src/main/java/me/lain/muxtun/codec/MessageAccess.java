package me.lain.muxtun.codec;

import java.util.UUID;
import io.netty.buffer.ByteBuf;

public interface MessageAccess
{

    @SuppressWarnings("unchecked")
    default <T extends Message> T cast()
    {
        return (T) this;
    }

    default void decode(ByteBuf buf) throws Exception
    {
    }

    default void encode(ByteBuf buf) throws Exception
    {
    }

    default ByteBuf getPayload()
    {
        throw new UnsupportedOperationException();
    }

    default UUID getStreamId()
    {
        throw new UnsupportedOperationException();
    }

    default Message setPayload(ByteBuf payload)
    {
        throw new UnsupportedOperationException();
    }

    default Message setStreamId(UUID streamId)
    {
        throw new UnsupportedOperationException();
    }

}

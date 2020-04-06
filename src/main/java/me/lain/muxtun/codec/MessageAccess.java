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

    default int getAck()
    {
        throw new UnsupportedOperationException();
    }

    default ByteBuf getBuf()
    {
        throw new UnsupportedOperationException();
    }

    default UUID getId()
    {
        throw new UnsupportedOperationException();
    }

    default int getSeq()
    {
        throw new UnsupportedOperationException();
    }

    default Message setAck(int ack)
    {
        throw new UnsupportedOperationException();
    }

    default Message setBuf(ByteBuf buf)
    {
        throw new UnsupportedOperationException();
    }

    default Message setId(UUID id)
    {
        throw new UnsupportedOperationException();
    }

    default Message setSeq(int seq)
    {
        throw new UnsupportedOperationException();
    }

}

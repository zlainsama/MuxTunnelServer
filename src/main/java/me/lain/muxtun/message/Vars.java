package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.util.ReferenceCounted;

class Vars
{

    @SuppressWarnings("unchecked")
    static <T> T safeDuplicate(T message)
    {
        if (message instanceof ByteBuf)
            return (T) ((ByteBuf) message).retainedDuplicate();
        else if (message instanceof ByteBufHolder)
            return (T) ((ByteBufHolder) message).retainedDuplicate();
        else if (message instanceof ReferenceCounted)
            return (T) ((ReferenceCounted) message).retain();
        return message;
    }

}

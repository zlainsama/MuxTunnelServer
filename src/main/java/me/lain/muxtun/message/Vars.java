package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;

class Vars
{

    static int getSize(ByteBuf buf)
    {
        if (buf == null)
            return 0;
        return buf.readableBytes();
    }

    static ByteBuf retainedDuplicate(ByteBuf buf)
    {
        if (buf == null)
            return null;
        return buf.retainedDuplicate();
    }

}

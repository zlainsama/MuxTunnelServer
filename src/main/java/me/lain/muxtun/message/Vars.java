package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;

class Vars
{

    static ByteBuf retainedDuplicate(ByteBuf buf)
    {
        if (buf == null)
            return null;
        return buf.retainedDuplicate();
    }

}

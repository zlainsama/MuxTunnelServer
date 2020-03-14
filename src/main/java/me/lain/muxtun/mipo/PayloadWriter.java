package me.lain.muxtun.mipo;

import io.netty.buffer.ByteBuf;

@FunctionalInterface
interface PayloadWriter
{

    boolean write(ByteBuf payload) throws Exception;

}

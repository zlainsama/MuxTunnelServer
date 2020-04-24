package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;

class Vars
{

    static int computeVarIntSize(int input)
    {
        for (int i = 1; i < 5; ++i)
            if ((input & -1 << i * 7) == 0)
                return i;

        return 5;
    }

    static int readVarInt(ByteBuf buf)
    {
        int i = 0;
        int j = 0;

        while (true)
        {
            byte b0 = buf.readByte();
            i |= (b0 & 127) << j++ * 7;
            if (j > 5)
                throw new RuntimeException("VarIntTooBig");

            if ((b0 & 128) != 128)
                break;
        }

        return i;
    }

    static ByteBuf writeVarInt(ByteBuf buf, int input)
    {
        while ((input & -128) != 0)
        {
            buf.writeByte(input & 127 | 128);
            input >>>= 7;
        }

        buf.writeByte(input);
        return buf;
    }

}

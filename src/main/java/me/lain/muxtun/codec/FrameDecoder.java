package me.lain.muxtun.codec;

import io.netty.handler.codec.LengthFieldBasedFrameDecoder;

public class FrameDecoder extends LengthFieldBasedFrameDecoder
{

    public FrameDecoder()
    {
        super(2097152 - 3, 0, 3, 0, 3);
    }

}

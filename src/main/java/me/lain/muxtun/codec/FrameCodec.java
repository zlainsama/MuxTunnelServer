package me.lain.muxtun.codec;

import io.netty.channel.CombinedChannelDuplexHandler;

public class FrameCodec extends CombinedChannelDuplexHandler<FrameDecoder, FrameEncoder>
{

    public FrameCodec()
    {
        init(new FrameDecoder(), new FrameEncoder());
    }

}

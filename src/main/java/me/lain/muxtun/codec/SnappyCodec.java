package me.lain.muxtun.codec;

import io.netty.channel.CombinedChannelDuplexHandler;

public class SnappyCodec extends CombinedChannelDuplexHandler<SnappyDecoder, SnappyEncoder>
{

    public SnappyCodec()
    {
        init(new SnappyDecoder(), new SnappyEncoder());
    }

}

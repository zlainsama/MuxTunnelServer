package me.lain.muxtun.codec;

import io.netty.channel.CombinedChannelDuplexHandler;

public class MessageCodec extends CombinedChannelDuplexHandler<MessageDecoder, MessageEncoder>
{

    public MessageCodec()
    {
        init(new MessageDecoder(), new MessageEncoder());
    }

}

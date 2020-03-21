package me.lain.muxtun.mipo;

import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import me.lain.muxtun.Shared;

class Vars
{

    static final AttributeKey<Throwable> ERROR_KEY = AttributeKey.newInstance("me.lain.muxtun.mipo.Vars#Error");
    static final AttributeKey<LinkSession> SESSION_KEY = AttributeKey.newInstance("me.lain.muxtun.mipo.Vars#Session");
    static final AttributeKey<PayloadWriter> WRITER_KEY = AttributeKey.newInstance("me.lain.muxtun.mipo.Vars#Writer");

    static final EventLoopGroup BOSS = Shared.NettyObjects.getOrCreateEventLoopGroup("bossGroup", 1);
    static final EventLoopGroup LINKS = Shared.NettyObjects.getOrCreateEventLoopGroup("linksGroup", 8);
    static final EventLoopGroup STREAMS = Shared.NettyObjects.getOrCreateEventLoopGroup("streamsGroup", 8);

}

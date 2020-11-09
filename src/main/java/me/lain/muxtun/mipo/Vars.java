package me.lain.muxtun.mipo;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.EventLoopGroup;
import io.netty.util.AttributeKey;
import io.netty.util.concurrent.EventExecutorGroup;
import me.lain.muxtun.Shared;
import me.lain.muxtun.codec.Message;

class Vars {

    static final ByteBuf TRUE_BUFFER = Unpooled.unreleasableBuffer(Unpooled.directBuffer(1, 1).writeBoolean(true).asReadOnly());
    static final ByteBuf FALSE_BUFFER = Unpooled.unreleasableBuffer(Unpooled.directBuffer(1, 1).writeBoolean(false).asReadOnly());

    static final AttributeKey<LinkContext> LINKCONTEXT_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#LinkContext");
    static final AttributeKey<StreamContext> STREAMCONTEXT_KEY = AttributeKey.valueOf("me.lain.muxtun.mipo.Vars#StreamContext");

    static final EventLoopGroup WORKERS = Shared.NettyObjects.getOrCreateEventLoopGroup("workersGroup", Math.max(4, Math.min(Runtime.getRuntime().availableProcessors() * 2, Short.MAX_VALUE)));
    static final EventExecutorGroup SESSIONS = Shared.NettyObjects.getOrCreateEventExecutorGroup("sessionsGroup", Math.max(4, Math.min(Runtime.getRuntime().availableProcessors(), Short.MAX_VALUE)));

    static final Message PLACEHOLDER = new Message() {

        @Override
        public Message copy() {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public void decode(ByteBuf buf) throws Exception {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public void encode(ByteBuf buf) throws Exception {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public int size() {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

        @Override
        public MessageType type() {
            throw new UnsupportedOperationException("PLACEHOLDER");
        }

    };

}

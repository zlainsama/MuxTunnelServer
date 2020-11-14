package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;
import me.lain.muxtun.message.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public interface Message extends MessageAccess {

    Message copy();

    void decode(ByteBuf buf) throws Exception;

    void encode(ByteBuf buf) throws Exception;

    int size();

    MessageType type();

    enum MessageType {

        PING(0x00, MessagePing::create),
        JOINSESSION(0x20, MessageJoinSession::create),
        OPENSTREAM(0x21, MessageOpenStream::create),
        OPENSTREAMUDP(0x22, MessageOpenStreamUDP::create),
        CLOSESTREAM(0x23, MessageCloseStream::create),
        DATASTREAM(0x24, MessageDataStream::create),
        ACKNOWLEDGE(0x25, MessageAcknowledge::create),
        UNKNOWN(0xFF, MessageType::createUnknown);

        private static final Map<Byte, MessageType> idMap = Collections.unmodifiableMap(Arrays.stream(values()).collect(Collectors.toMap(MessageType::getId, Function.identity())));
        private final byte id;
        private final MessageFactory factory;

        MessageType(int id, MessageFactory factory) {
            this.id = (byte) id;
            this.factory = factory;
        }

        private static Message createUnknown() {
            throw new UnsupportedOperationException("UnknownMessageType");
        }

        public static MessageType find(byte id) {
            return idMap.getOrDefault(id, UNKNOWN);
        }

        public Message create() {
            return factory.create();
        }

        public byte getId() {
            return id;
        }

    }

    @FunctionalInterface
    interface MessageFactory {

        Message create();

    }

}

package me.lain.muxtun.codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import io.netty.buffer.ByteBuf;
import me.lain.muxtun.message.MessageAcknowledge;
import me.lain.muxtun.message.MessageCloseStream;
import me.lain.muxtun.message.MessageDataStream;
import me.lain.muxtun.message.MessageJoinSession;
import me.lain.muxtun.message.MessageOpenStream;
import me.lain.muxtun.message.MessageOpenStreamUDP;
import me.lain.muxtun.message.MessagePing;

public interface Message extends MessageAccess
{

    @FunctionalInterface
    interface MessageFactory
    {

        Message create();

    }

    enum MessageType
    {

        PING(0x00, MessagePing::create),
        JOINSESSION(0x20, MessageJoinSession::create),
        OPENSTREAM(0x21, MessageOpenStream::create),
        OPENSTREAMUDP(0x22, MessageOpenStreamUDP::create),
        CLOSESTREAM(0x23, MessageCloseStream::create),
        DATASTREAM(0x24, MessageDataStream::create),
        ACKNOWLEDGE(0x25, MessageAcknowledge::create),
        UNKNOWN(0xFF, MessageType::createUnknown);

        private static final Map<Byte, MessageType> idMap = Collections.unmodifiableMap(Arrays.stream(values()).collect(Collectors.toMap(MessageType::getId, Function.identity())));

        private static Message createUnknown()
        {
            throw new UnsupportedOperationException("UnknownMessageType");
        }

        public static MessageType find(byte id)
        {
            return idMap.getOrDefault(id, UNKNOWN);
        }

        private final byte id;
        private final MessageFactory factory;

        private MessageType(int id, MessageFactory factory)
        {
            this.id = (byte) id;
            this.factory = factory;
        }

        public Message create()
        {
            return factory.create();
        }

        public byte getId()
        {
            return id;
        }

    }

    Message copy();

    void decode(ByteBuf buf) throws Exception;

    void encode(ByteBuf buf) throws Exception;

    MessageType type();

}

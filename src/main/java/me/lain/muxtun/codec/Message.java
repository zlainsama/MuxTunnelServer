package me.lain.muxtun.codec;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import me.lain.muxtun.message.MessageAuth;
import me.lain.muxtun.message.MessageAuthReq;
import me.lain.muxtun.message.MessageAuthReq3;
import me.lain.muxtun.message.MessageData;
import me.lain.muxtun.message.MessageDrop;
import me.lain.muxtun.message.MessageOpen;
import me.lain.muxtun.message.MessageOpenUDP;
import me.lain.muxtun.message.MessagePing;
import me.lain.muxtun.message.MessageSnappy;

public interface Message extends MessageAccess
{

    @FunctionalInterface
    interface MessageFactory
    {

        Message create();

    }

    enum MessageType
    {

        PING(0x31, MessagePing::create),
        OPEN(0x32, MessageOpen::create),
        DATA(0x33, MessageData::create),
        DROP(0x34, MessageDrop::create),
        OPENUDP(0x35, MessageOpenUDP::create),
        AUTH(0x71, MessageAuth::create),
        AUTHREQ(0x72, MessageAuthReq::create),
        AUTHREQ3(0x73, MessageAuthReq3::create),
        SNAPPY(0xB3, MessageSnappy::create),
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

    MessageType type();

}

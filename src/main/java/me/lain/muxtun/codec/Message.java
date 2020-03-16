package me.lain.muxtun.codec;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import io.netty.buffer.ByteBuf;

public final class Message
{

    public enum MessageType
    {

        Ping((byte) 0x31),
        Open((byte) 0x32),
        Data((byte) 0x33),
        Drop((byte) 0x34),
        OpenUDP((byte) 0x35),
        Auth((byte) 0x71),
        AuthReq((byte) 0x72),
        AuthReq_3((byte) 0x73),
        Snappy((byte) 0xB3),
        Unknown((byte) 0xFF);

        private static final Map<Byte, MessageType> idMap = new HashMap<>();

        static
        {
            for (MessageType type : values())
                idMap.put(type.getId(), type);
        }

        public static MessageType find(byte id)
        {
            return idMap.getOrDefault(id, Unknown);
        }

        private final byte id;

        private MessageType(byte id)
        {
            this.id = id;
        }

        public byte getId()
        {
            return id;
        }

    }

    private MessageType type;
    private UUID streamId;
    private ByteBuf payload;

    public ByteBuf getPayload()
    {
        return payload;
    }

    public UUID getStreamId()
    {
        return streamId;
    }

    public MessageType getType()
    {
        return type;
    }

    public Message setPayload(ByteBuf payload)
    {
        this.payload = payload;
        return this;
    }

    public Message setStreamId(UUID streamId)
    {
        this.streamId = streamId;
        return this;
    }

    public Message setType(MessageType type)
    {
        this.type = type;
        return this;
    }

}

package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCounted;
import me.lain.muxtun.codec.Message;

import java.util.UUID;

public class MessageDataStream implements Message, ReferenceCounted {

    private int seq;
    private int req;
    private UUID id;
    private ByteBuf buf;

    private MessageDataStream() {
    }

    public static MessageDataStream create() {
        return new MessageDataStream();
    }

    @Override
    public Message copy() {
        return type().create().setSeq(getSeq()).setReq(getReq()).setId(getId()).setBuf(Vars.retainedDuplicate(getBuf()));
    }

    @Override
    public void decode(ByteBuf buf) throws Exception {
        setSeq(buf.readInt());
        setReq(buf.readInt());
        setId(new UUID(buf.readLong(), buf.readLong()));
        setBuf(buf.readBytes(buf.readableBytes()));
    }

    @Override
    public void encode(ByteBuf buf) throws Exception {
        int _seq = getSeq();
        buf.writeInt(_seq);

        int _req = getReq();
        buf.writeInt(_req);

        UUID _id = getId();
        buf.writeLong(_id.getMostSignificantBits()).writeLong(_id.getLeastSignificantBits());

        ByteBuf _buf = getBuf();
        buf.writeBytes(_buf);
    }

    @Override
    public ByteBuf getBuf() {
        return buf;
    }

    @Override
    public MessageDataStream setBuf(ByteBuf buf) {
        this.buf = buf;
        return this;
    }

    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public MessageDataStream setId(UUID id) {
        this.id = id;
        return this;
    }

    @Override
    public int getReq() {
        return req;
    }

    @Override
    public MessageDataStream setReq(int req) {
        this.req = req;
        return this;
    }

    @Override
    public int getSeq() {
        return seq;
    }

    @Override
    public MessageDataStream setSeq(int seq) {
        this.seq = seq;
        return this;
    }

    @Override
    public int refCnt() {
        if (buf != null)
            return buf.refCnt();
        return 0;
    }

    @Override
    public boolean release() {
        if (buf != null)
            return buf.release();
        return false;
    }

    @Override
    public boolean release(int decrement) {
        if (buf != null)
            return buf.release(decrement);
        return false;
    }

    @Override
    public MessageDataStream retain() {
        if (buf != null)
            buf.retain();
        return this;
    }

    @Override
    public MessageDataStream retain(int increment) {
        if (buf != null)
            buf.retain(increment);
        return this;
    }

    @Override
    public int size() {
        return 24 + Vars.getSize(getBuf());
    }

    @Override
    public MessageDataStream touch() {
        if (buf != null)
            buf.touch();
        return this;
    }

    @Override
    public MessageDataStream touch(Object hint) {
        if (buf != null)
            buf.touch(hint);
        return this;
    }

    @Override
    public MessageType type() {
        return MessageType.DATASTREAM;
    }

}

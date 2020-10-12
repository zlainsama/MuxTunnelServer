package me.lain.muxtun.message;

import io.netty.buffer.ByteBuf;
import me.lain.muxtun.codec.Message;

public class MessageAcknowledge implements Message {

    private int ack;
    private int sack;

    private MessageAcknowledge() {
    }

    public static MessageAcknowledge create() {
        return new MessageAcknowledge();
    }

    @Override
    public Message copy() {
        return type().create().setAck(getAck()).setSAck(getSAck());
    }

    @Override
    public void decode(ByteBuf buf) throws Exception {
        if (buf.readableBytes() > 4) {
            setAck(buf.readInt());
            setSAck(buf.readInt());
        } else {
            setAck(buf.readInt());
            setSAck(getAck() - 1);
        }
    }

    @Override
    public void encode(ByteBuf buf) throws Exception {
        if (getSAck() - getAck() > 0) {
            int _ack = getAck();
            buf.writeInt(_ack);

            int _sack = getSAck();
            buf.writeInt(_sack);
        } else {
            int _ack = getAck();
            buf.writeInt(_ack);
        }
    }

    @Override
    public int getAck() {
        return ack;
    }

    @Override
    public MessageAcknowledge setAck(int ack) {
        this.ack = ack;
        return this;
    }

    @Override
    public int getSAck() {
        return sack;
    }

    @Override
    public MessageAcknowledge setSAck(int sack) {
        this.sack = sack;
        return this;
    }

    @Override
    public int size() {
        return getSAck() - getAck() > 0 ? 8 : 4;
    }

    @Override
    public MessageType type() {
        return MessageType.ACKNOWLEDGE;
    }

}

package me.lain.muxtun.codec;

import io.netty.buffer.ByteBuf;

import java.util.UUID;

public interface MessageAccess {

    @SuppressWarnings("unchecked")
    default <T extends Message> T cast() {
        return (T) this;
    }

    default int getAck() {
        throw new UnsupportedOperationException();
    }

    default ByteBuf getBuf() {
        throw new UnsupportedOperationException();
    }

    default UUID getId() {
        throw new UnsupportedOperationException();
    }

    default UUID getId2() {
        throw new UnsupportedOperationException();
    }

    default int getReq() {
        throw new UnsupportedOperationException();
    }

    default int getSAck() {
        throw new UnsupportedOperationException();
    }

    default int getSeq() {
        throw new UnsupportedOperationException();
    }

    default Message setAck(int ack) {
        throw new UnsupportedOperationException();
    }

    default Message setBuf(ByteBuf buf) {
        throw new UnsupportedOperationException();
    }

    default Message setId(UUID id) {
        throw new UnsupportedOperationException();
    }

    default Message setId2(UUID id2) {
        throw new UnsupportedOperationException();
    }

    default Message setReq(int req) {
        throw new UnsupportedOperationException();
    }

    default Message setSAck(int sack) {
        throw new UnsupportedOperationException();
    }

    default Message setSeq(int seq) {
        throw new UnsupportedOperationException();
    }

}

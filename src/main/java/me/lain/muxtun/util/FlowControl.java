package me.lain.muxtun.util;

import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class FlowControl {

    private final Object local;
    private final Object remote;
    private final int initialWindowSize;

    private volatile int window;
    private volatile int sequence;
    private volatile int lastAck;
    private volatile int expect;

    public FlowControl() {
        this(4096);
    }

    public FlowControl(int windowSize) {
        local = new Object();
        remote = new Object();
        initialWindowSize = windowSize;
        window = windowSize;
    }

    public int acknowledge(IntStream outbound, IntConsumer remover, int ack, int sack) {
        synchronized (local) {
            int inc = Math.max(0, ack - lastAck);
            lastAck += inc;
            int sackDiff = sack - ack;
            outbound.map(i -> i - ack).filter(i -> i < 0 || i == sackDiff).map(i -> ack + i).forEach(remover);
            return window += inc;
        }
    }

    public int expect() {
        return expect;
    }

    public int initialWindowSize() {
        return initialWindowSize;
    }

    public boolean inRange(int seq) {
        int diff = seq - expect;
        return diff >= 0 && diff < initialWindowSize;
    }

    public int lastAck() {
        return lastAck;
    }

    public int sequence() {
        return sequence;
    }

    public int tryAdvanceSequence(IntPredicate consumer) {
        synchronized (local) {
            if (window > 0 && consumer.test(sequence)) {
                window -= 1;
                sequence += 1;
            }

            return window;
        }
    }

    public int updateReceived(IntStream inbound, IntConsumer issuer, IntConsumer discarder, IntBinaryOperator operator) {
        synchronized (remote) {
            int base = expect;
            inbound.map(i -> i - base).sorted().map(i -> base + i).sequential().filter(i -> {
                if (i == expect) {
                    expect += 1;
                    return true;
                } else if (expect - i > 0) {
                    discarder.accept(i);
                } else {
                    operator.applyAsInt(i, expect);
                }

                return false;
            }).forEach(issuer);

            return expect;
        }
    }

    public int window() {
        return window;
    }

}

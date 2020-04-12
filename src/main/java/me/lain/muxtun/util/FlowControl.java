package me.lain.muxtun.util;

import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.IntConsumer;
import java.util.stream.IntStream;

public class FlowControl
{

    private final Object local;
    private final Object remote;
    private final int initialWindowSize;
    private volatile int window;
    private volatile int sequence;
    private volatile int lastAck;
    private volatile int expect;

    public FlowControl()
    {
        this(256);
    }

    public FlowControl(int windowSize)
    {
        local = new Object();
        remote = new Object();
        initialWindowSize = windowSize;
        window = windowSize;
    }

    public Optional<IntStream> acknowledge(int ack)
    {
        synchronized (local)
        {
            int inc = ack - lastAck;

            if (inc > 0 && sequence - ack >= 0)
            {
                int base = lastAck;
                IntStream toComplete = IntStream.range(0, inc).map(i -> base + i);
                lastAck = ack;
                window += inc;
                return Optional.of(toComplete);
            }

            return Optional.empty();
        }
    }

    public int initialWindowSize()
    {
        return initialWindowSize;
    }

    public boolean inRange(int seq)
    {
        int diff = seq - expect;
        return diff >= 0 && diff < initialWindowSize;
    }

    public OptionalInt tryAdvanceSequence()
    {
        synchronized (local)
        {
            if (window > 0)
            {
                int seq = sequence;
                window -= 1;
                sequence += 1;
                return OptionalInt.of(seq);
            }

            return OptionalInt.empty();
        }
    }

    public int updateReceived(IntStream received, IntConsumer packetIssuer, IntConsumer packetDiscarder)
    {
        synchronized (remote)
        {
            int base = expect;
            received.map(i -> i - base).sorted().map(i -> base + i).sequential().filter(i -> {
                if (i == expect)
                {
                    expect += 1;
                    return true;
                }
                else if (expect - i > 0)
                {
                    packetDiscarder.accept(i);
                }

                return false;
            }).forEach(packetIssuer);

            return expect;
        }
    }

    public int window()
    {
        return window;
    }

}

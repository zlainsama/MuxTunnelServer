package me.lain.muxtun.util;

import java.util.function.IntConsumer;
import java.util.function.IntPredicate;
import java.util.stream.IntStream;

public class FlowControl
{

    private final Object local;
    private final Object remote;
    private final int initialWindowSize;
    private volatile int window;
    private volatile int sequence;
    private volatile int expect;

    public FlowControl()
    {
        this(1024);
    }

    public FlowControl(int windowSize)
    {
        local = new Object();
        remote = new Object();
        initialWindowSize = windowSize;
        window = windowSize;
    }

    public int acknowledge(IntStream outbound, IntPredicate remover, int ack)
    {
        synchronized (local)
        {
            return window += Math.toIntExact(outbound.map(i -> i - ack).filter(i -> i < 0).map(i -> ack + i).filter(remover).count());
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

    public int tryAdvanceSequence(IntPredicate consumer)
    {
        synchronized (local)
        {
            if (window > 0 && consumer.test(sequence))
            {
                window -= 1;
                sequence += 1;
            }

            return window;
        }
    }

    public int updateReceived(IntStream inbound, IntConsumer issuer, IntConsumer discarder)
    {
        synchronized (remote)
        {
            int base = expect;
            inbound.map(i -> i - base).sorted().map(i -> base + i).sequential().filter(i -> {
                if (i == expect)
                {
                    expect += 1;
                    return true;
                }
                else if (expect - i > 0)
                {
                    discarder.accept(i);
                }

                return false;
            }).forEach(issuer);

            return expect;
        }
    }

    public int window()
    {
        return window;
    }

}

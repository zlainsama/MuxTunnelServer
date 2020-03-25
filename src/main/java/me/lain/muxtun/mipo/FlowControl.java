package me.lain.muxtun.mipo;

import java.util.function.Consumer;
import java.util.function.IntUnaryOperator;

class FlowControl
{

    private final int maxIncrement;
    private final Consumer<Boolean> localStateUpdater;
    private final Consumer<Integer> remoteWindowUpdater;
    private final Object local;
    private final Object remote;

    private int window;
    private int received;
    private int threshold;
    private int increment;

    FlowControl(Consumer<Boolean> localStateUpdater, Consumer<Integer> remoteWindowUpdater)
    {
        this(65536, 1048576, localStateUpdater, remoteWindowUpdater);
    }

    FlowControl(int initialWindowSize, int maxIncrement, Consumer<Boolean> localStateUpdater, Consumer<Integer> remoteWindowUpdater)
    {
        this.window = initialWindowSize;
        this.received = 0;
        this.threshold = initialWindowSize / 2;
        this.increment = initialWindowSize;
        this.maxIncrement = maxIncrement;
        this.localStateUpdater = localStateUpdater;
        this.remoteWindowUpdater = remoteWindowUpdater;
        this.local = new Object();
        this.remote = new Object();
    }

    int updateReceived(IntUnaryOperator updateFunction)
    {
        synchronized (remote)
        {
            return received = updateFunction.andThen(i -> {
                if (i >= threshold)
                {
                    remoteWindowUpdater.accept(increment < maxIncrement ? Math.min(increment + threshold, maxIncrement) : increment);
                    i -= threshold;
                    threshold = increment;
                    increment = increment < maxIncrement ? Math.min(increment * 2, maxIncrement) : increment;
                }
                return i;
            }).applyAsInt(received);
        }
    }

    int updateWindowSize(IntUnaryOperator updateFunction)
    {
        synchronized (local)
        {
            return window = updateFunction.andThen(i -> {
                localStateUpdater.accept(i > 0);
                return i;
            }).applyAsInt(window);
        }
    }

}

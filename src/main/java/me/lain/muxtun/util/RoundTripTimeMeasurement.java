package me.lain.muxtun.util;

import java.util.OptionalLong;
import java.util.function.LongPredicate;

public class RoundTripTimeMeasurement
{

    protected volatile OptionalLong startTime = OptionalLong.empty();
    protected volatile OptionalLong lastResult = OptionalLong.empty();

    public OptionalLong complete()
    {
        if (startTime.isPresent())
        {
            synchronized (this)
            {
                if (startTime.isPresent())
                {
                    lastResult = OptionalLong.of(System.currentTimeMillis() - startTime.getAsLong());
                    startTime = OptionalLong.empty();
                }
            }
        }

        return lastResult;
    }

    public OptionalLong get()
    {
        return lastResult;
    }

    public boolean initiate()
    {
        if (!startTime.isPresent())
        {
            synchronized (this)
            {
                if (!startTime.isPresent())
                {
                    startTime = OptionalLong.of(System.currentTimeMillis());
                    return true;
                }
            }
        }

        return false;
    }

    public OptionalLong updateIf(LongPredicate predicate)
    {
        if (startTime.isPresent())
        {
            synchronized (this)
            {
                if (startTime.isPresent())
                {
                    long result = System.currentTimeMillis() - startTime.getAsLong();
                    if (predicate.test(result))
                        return lastResult = OptionalLong.of(result);
                }
            }
        }

        return OptionalLong.empty();
    }

}

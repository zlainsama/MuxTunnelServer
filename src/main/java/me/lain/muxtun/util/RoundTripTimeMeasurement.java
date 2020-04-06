package me.lain.muxtun.util;

import java.util.OptionalLong;

public class RoundTripTimeMeasurement
{

    private volatile OptionalLong startTime = OptionalLong.empty();
    private volatile OptionalLong lastResult = OptionalLong.empty();

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

}

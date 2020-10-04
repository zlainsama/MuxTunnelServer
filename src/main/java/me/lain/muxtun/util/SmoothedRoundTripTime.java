package me.lain.muxtun.util;

import java.util.concurrent.atomic.AtomicReference;

//RFC6298
public class SmoothedRoundTripTime
{

    private static final float ALPHA = 1F / 8F;
    private static final float ONEMINUSALPHA = 1F - ALPHA;
    private static final float BETA = 1F / 4F;
    private static final float ONEMINUSBETA = 1F - BETA;
    private static final float K = 4F;

    private static final int INDEX_SRTT = 0;
    private static final int INDEX_RTTVAR = 1;
    private static final int INDEX_RTO = 2;
    private static final int SIZE_ARRAY = 3;

    private final AtomicReference<long[]> _state = new AtomicReference<>(new long[SIZE_ARRAY]);

    public long get()
    {
        return Math.max(1L, _state.get()[INDEX_SRTT]);
    }

    public void reset()
    {
        _state.set(new long[SIZE_ARRAY]);
    }

    public long rto()
    {
        return Math.max(1000L, _state.get()[INDEX_RTO]);
    }

    public long updateAndGet(long RTT)
    {
        long[] os = null, ns = new long[SIZE_ARRAY]; // oldState, newState

        do
        {
            os = _state.get();

            if (os[INDEX_SRTT] == 0L)
            {
                long SRTT = RTT;
                long RTTVAR = RTT / 2L;
                long RTO = SRTT + Math.max(1000L, Math.round(K * RTTVAR));

                ns[INDEX_SRTT] = SRTT;
                ns[INDEX_RTTVAR] = RTTVAR;
                ns[INDEX_RTO] = RTO;
            }
            else
            {
                long SRTT = os[INDEX_SRTT];
                long RTTVAR = os[INDEX_RTTVAR];
                long RTO = os[INDEX_RTO];

                RTTVAR = Math.round(ONEMINUSBETA * RTTVAR) + Math.round(BETA * Math.abs(SRTT - RTT));
                SRTT = Math.round(ONEMINUSALPHA * SRTT) + Math.round(ALPHA * RTT);
                RTO = SRTT + Math.max(1000L, Math.round(K * RTTVAR));

                ns[INDEX_SRTT] = SRTT;
                ns[INDEX_RTTVAR] = RTTVAR;
                ns[INDEX_RTO] = RTO;
            }
        }
        while (!_state.compareAndSet(os, ns));

        return Math.max(1L, ns[INDEX_SRTT]);
    }

    public long var()
    {
        return Math.max(0L, _state.get()[INDEX_RTTVAR]);
    }

}

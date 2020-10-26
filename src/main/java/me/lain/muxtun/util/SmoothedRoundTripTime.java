package me.lain.muxtun.util;

//RFC6298
public class SmoothedRoundTripTime {

    protected static final float ALPHA = 1F / 8F;
    protected static final float ONEMINUSALPHA = 1F - ALPHA;
    protected static final float BETA = 1F / 4F;
    protected static final float ONEMINUSBETA = 1F - BETA;
    protected static final float K = 4F;

    protected volatile long SRTT;
    protected volatile long RTTVAR;
    protected volatile long RTO;

    public long get() {
        return Math.max(1L, SRTT);
    }

    public synchronized void reset() {
        SRTT = 0L;
        RTTVAR = 0L;
        RTO = 0L;
    }

    public long rto() {
        return Math.max(1000L, RTO);
    }

    public synchronized long updateAndGet(long RTT) {
        if (SRTT == 0L) {
            SRTT = RTT;
            RTTVAR = RTT / 2L;
            RTO = SRTT + Math.max(1000L, Math.round(K * RTTVAR));
        } else {
            RTTVAR = Math.round(ONEMINUSBETA * RTTVAR) + Math.round(BETA * Math.abs(SRTT - RTT));
            SRTT = Math.round(ONEMINUSALPHA * SRTT) + Math.round(ALPHA * RTT);
            RTO = SRTT + Math.max(1000L, Math.round(K * RTTVAR));
        }

        return get();
    }

    public long var() {
        return Math.max(0L, RTTVAR);
    }

}

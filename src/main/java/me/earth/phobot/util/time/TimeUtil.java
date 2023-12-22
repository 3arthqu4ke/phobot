package me.earth.phobot.util.time;

import lombok.experimental.UtilityClass;

import java.util.concurrent.TimeUnit;

@UtilityClass
public class TimeUtil {
    public static final long NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1L);

    public static long getMillis() {
        return System.nanoTime() / NANOS_PER_MS;
    }

    public static boolean isTimeStampOlderThan(long timeStamp, long ms) {
        return getPassedTimeSince(timeStamp) >= ms;
    }

    public static long getPassedTimeSince(long timeStamp) {
        return getMillis() - timeStamp;
    }

}

package me.earth.phobot.util.time;

import lombok.Getter;
import lombok.Setter;

public interface StopWatch {
    long getTimeStamp();

    void setTimeStamp(long ms);

    default long getPassedTime() {
        return TimeUtil.getPassedTimeSince(getTimeStamp());
    }

    default boolean passed(long ms) {
        return TimeUtil.isTimeStampOlderThan(this.getTimeStamp(), ms);
    }

    default void reset() {
        setTimeStamp(TimeUtil.getMillis());
    }

    @Getter
    @Setter
    class ForSingleThread implements StopWatch {
        private long timeStamp;
    }

    @Getter
    @Setter
    class ForMultipleThreads implements StopWatch {
        private volatile long timeStamp;
    }

}

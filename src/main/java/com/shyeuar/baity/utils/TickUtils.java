package com.shyeuar.baity.utils;

import java.util.concurrent.TimeUnit;

public final class TickUtils {

    public static final int TICKS_PER_SECOND = 20;
    public static final int MS_PER_SECOND = 1000;
    public static final int MS_PER_TICK = MS_PER_SECOND / TICKS_PER_SECOND;

    private TickUtils() {
    }

    public static int fromTime(int duration, TimeUnit unit) {
        return (int) (unit.toMillis(duration) / MS_PER_TICK);
    }

    public static int toTime(int ticks, TimeUnit unit) {
        return (int) unit.convert((long) ticks * MS_PER_TICK, TimeUnit.MILLISECONDS);
    }
}

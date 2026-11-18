package com.akif.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private final Clock base;
    private volatile Duration offset = Duration.ZERO;

    public MutableClock(Clock base) {
        this.base = base;
    }

    public void advance(Duration duration) {
        offset = offset.plus(duration);
    }

    public void reset() {
        offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        MutableClock clone = new MutableClock(base.withZone(zone));
        clone.offset = offset;
        return clone;
    }

    @Override
    public Instant instant() {
        return base.instant().plus(offset);
    }

    public static MutableClock systemUtc() {
        return new MutableClock(Clock.system(ZoneOffset.UTC));
    }
}

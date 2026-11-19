package com.akif.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

public class MutableClock extends Clock {

    private final Clock initialBase;
    private volatile Clock base;
    private volatile Duration offset = Duration.ZERO;

    public MutableClock(Clock base) {
        this.initialBase = base;
        this.base = base;
    }

    public void advance(Duration duration) {
        offset = offset.plus(duration);
    }

    public void pinTo(Instant instant) {
        base = Clock.fixed(instant, initialBase.getZone());
        offset = Duration.ZERO;
    }

    public void reset() {
        base = initialBase;
        offset = Duration.ZERO;
    }

    @Override
    public ZoneId getZone() {
        return base.getZone();
    }

    @Override
    public Clock withZone(ZoneId zone) {
        MutableClock clone = new MutableClock(initialBase.withZone(zone));
        clone.base = base.withZone(zone);
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

    public static MutableClock fixedAt(Instant instant) {
        return new MutableClock(Clock.fixed(instant, ZoneOffset.UTC));
    }
}

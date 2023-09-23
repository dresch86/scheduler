package org.ose.scheduler.data;

import java.time.LocalTime;

import org.ose.scheduler.structures.TimeInterval;

public class Availability implements Comparable<Availability> {

    private final TimeInterval tiTimePeriod;

    public Availability(LocalTime start, LocalTime end) {
        this.tiTimePeriod = new TimeInterval(start, end);
    }

    public TimeInterval getInterval() {
        return this.tiTimePeriod;
    }

    public boolean contains(TimeInterval interval) {
        return tiTimePeriod.contains(interval);
    }

    @Override
    public int compareTo(Availability o) {
        return tiTimePeriod.compareTo(o.getInterval());
    }
}
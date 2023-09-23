package org.ose.scheduler.structures;

import java.time.LocalTime;

public class TimeInterval implements Comparable<TimeInterval> {
    private final LocalTime ltEnd;
    
    private final LocalTime ltStart;

    public TimeInterval(LocalTime start, LocalTime end) {
        this.ltEnd = end;
        this.ltStart = start;
    }

    public boolean contains(TimeInterval interval) {
        return (this.ltStart.compareTo(interval.getStart()) <= 0) 
            && (this.ltEnd.compareTo(interval.getEnd()) >= 0);
    }

    public LocalTime[] getInterval() {
        return new LocalTime[] {ltStart, ltEnd};
    }

    public LocalTime getStart() {
        return this.ltStart;
    }

    public LocalTime getEnd() {
        return this.ltEnd;
    }

    @Override
    public int compareTo(TimeInterval o) {
        int iLowerResult = this.ltStart.compareTo(o.getStart());
        return (iLowerResult == 0) ? this.ltEnd.compareTo(o.getEnd()) : iLowerResult;
    }
}
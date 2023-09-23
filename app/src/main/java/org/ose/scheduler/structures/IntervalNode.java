package org.ose.scheduler.structures;

import java.util.Set;

import java.time.LocalTime;

public interface IntervalNode<T extends Comparable<T>> extends Comparable<IntervalNode<T>> {
    void addData(T data);
    void addData(Set<T> data);
    LocalTime getLowerBound();
    LocalTime getUpperBound();
    TimeInterval getTimeInterval();
    boolean contains(TimeInterval interval);
}
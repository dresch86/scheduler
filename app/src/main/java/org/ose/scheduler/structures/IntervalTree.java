package org.ose.scheduler.structures;

import java.util.Set;
import java.util.SortedSet;

public interface IntervalTree<T extends Comparable<T>> {
    void addNode(TimeInterval interval, T data);
    SortedSet<T> overlaps(TimeInterval interval);
    IntervalNode<T> exists(TimeInterval interval);
    void addNode(TimeInterval interval, Set<T> data);
}
package org.ose.scheduler.structures;

import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Comparator;

import java.time.LocalTime;

import org.ose.scheduler.Common;

public class AVLIntervalNode<T extends Comparable<T>> implements IntervalNode<T> {
    private int iHeight;

    private LocalTime ltMin;
    private LocalTime ltMax;

    private TimeInterval tiPeriod;
    private AVLIntervalNode<T> left;
    private AVLIntervalNode<T> right;

    private final Set<T> hsData = new HashSet<>(10);

    public AVLIntervalNode(TimeInterval timePeriod) {
        this.iHeight = 0;
        this.tiPeriod = timePeriod;

        this.ltMin = this.tiPeriod.getStart();
        this.ltMax = this.tiPeriod.getEnd();

        this.left = null;
        this.right = null;
    }

    public void incrementHeight() {
        this.iHeight += 1;
    }

    public void setHeight(int height) {
        this.iHeight = height;
    }

    public int getHeight() {
        return this.iHeight;
    }

    public void setLeft(AVLIntervalNode<T> left) {
        this.left = left;
    }

    public AVLIntervalNode<T> getLeft() {
        return this.left;
    }

    public void setRight(AVLIntervalNode<T> right) {
        this.right = right;
    }

    public AVLIntervalNode<T> getRight() {
        return this.right;
    }

    public void addData(T data) {
        hsData.add(data);
    }

    public void addData(Set<T> data) {
        hsData.addAll(data);
    }

    public Iterator<T> getDataIterator() {
        return hsData.iterator();
    }

    public LocalTime getLowerBound() {
        return this.tiPeriod.getStart();
    }

    public LocalTime getUpperBound() {
        return this.tiPeriod.getEnd();
    }

    public LocalTime getMax() {
        return this.ltMax;
    }

    public LocalTime getMin() {
        return this.ltMin;
    }

    public void updateMinMax() {
        List<LocalTime> liTimes;

        LocalTime ltLocalMaxLeft = (this.left == null) ? this.ltMax : this.left.getMax();
        LocalTime ltLocalMaxRight = (this.right == null) ? this.ltMax : this.right.getMax();
        liTimes = List.of(this.ltMax, ltLocalMaxLeft, ltLocalMaxRight);

        LocalTime ltLocalMax = liTimes.stream().max(Comparator.naturalOrder()).get();
        this.ltMax = ltLocalMax;

        LocalTime ltLocalMinLeft = (this.left == null) ? this.ltMin : this.left.getMin();
        LocalTime ltLocalMinRight = (this.right == null) ? this.ltMin : this.right.getMin();
        liTimes = List.of(this.ltMin, ltLocalMinLeft, ltLocalMinRight);

        LocalTime ltLocalMin = liTimes.stream().min(Comparator.naturalOrder()).get();
        this.ltMin = ltLocalMin;
    }

    public int getBalanceFactor() {
        int iHeightLeft = (this.left == null) ? -1 : this.left.getHeight();
        int iHeightRight = (this.right == null) ? -1 : this.right.getHeight();

        return (iHeightLeft + 1) - (iHeightRight + 1);
    }

    @Override
    public boolean contains(TimeInterval timePeriod) {
        return this.tiPeriod.contains(timePeriod);
    }

    @Override
    public TimeInterval getTimeInterval() {
        return this.tiPeriod;
    }

    @Override
    public String toString() {
        StringBuilder sbBuffer = new StringBuilder();
        sbBuffer.append("[ ")
        .append(Common.prettyTime(this.tiPeriod.getStart()))
        .append(", ")
        .append(Common.prettyTime(this.tiPeriod.getEnd()))
        .append(" ]");

        return sbBuffer.toString();
    }

    @Override
    public int compareTo(IntervalNode<T> headNode) {
        return this.tiPeriod.compareTo(headNode.getTimeInterval());
    }
}
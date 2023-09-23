package org.ose.scheduler.data;

import java.util.Objects;

import java.time.LocalTime;

import org.ose.scheduler.structures.TimeInterval;

public class TimeBlock implements Comparable<TimeBlock> {
    private int iId;

    private int iStatus;

    private String sDay;

    private String sLabel;

    private String sLocation;

    private double dblTimeMetric;

    private TimeInterval tiPeriod;

    private Employee emplAssignedEmployee = null;

    private final StringBuilder sbPrinter = new StringBuilder();

    public TimeBlock(int id, String label) {
        this.iId = id;
        this.sLabel = label;
    }

    public TimeBlock setTimeMetric(double metric) {
        this.dblTimeMetric = metric;
        return this;
    }

    public double getTimeMetric() {
        return this.dblTimeMetric;
    }

    public TimeBlock setLocation(String location) {
        this.sLocation = location;
        return this;
    }

    public String getLocation() {
        return this.sLocation;
    }

    public TimeBlock setStatus(int status) {
        this.iStatus = status;
        return this;
    }

    public int getStatus() {
        return this.iStatus;
    }

    public int getId() {
        return this.iId;
    }

    public String getLabel() {
        return this.sLabel;
    }

    public String getDay() {
        return this.sDay;
    }

    public TimeInterval getInterval() {
        return this.tiPeriod;
    }

    public TimeBlock setDayAndTime(String day, LocalTime start, LocalTime end) {
        this.tiPeriod = new TimeInterval(start, end);
        this.sDay = day;

        return this;
    }

    public void setAssignedEmployee(Employee employee) {
        this.emplAssignedEmployee = employee;
    }

    public Employee getAssignedEmployee() {
        return this.emplAssignedEmployee;
    }

    public TimeBlock makePrintable() {
        sbPrinter.append("[@id = ")
        .append(iId)
        .append("; @label = ")
        .append(sLabel)
        .append("; @time_metric = ")
        .append(dblTimeMetric)
        .append("; @day = ")
        .append(sDay)
        .append("]");

        return this;
    }

    @Override
    public String toString() {
        return sbPrinter.toString();
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.iId);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TimeBlock)) {
            return false;
        }

        return this.tiPeriod.compareTo(((TimeBlock) o).getInterval()) == 0;
    }

    @Override
    public int compareTo(TimeBlock o) {
        return this.tiPeriod.compareTo(o.getInterval());
    }
}
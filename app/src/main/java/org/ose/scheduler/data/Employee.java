package org.ose.scheduler.data;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.ose.scheduler.Common;
import org.ose.scheduler.structures.TimeInterval;

public class Employee implements Comparable<Employee> {
    private int iId;

    private int iPriority;

    private String sLastName;

    private String sFirstName;

    private double dblAssignedTimeMetric;

    private double dblRequestedTimeMetric;

    private Set<String> hsQualifications;

    private List<TimeBlock> liAssignedBlocks;

    private Map<String, List<Availability>> mAvailability;

    private final StringBuilder sbOutputHandler = new StringBuilder();

    private static final Logger logger = LogManager.getLogger(Employee.class);

    public Employee(int id) {
        this.iId = id;
        this.dblAssignedTimeMetric = 0;
        this.dblRequestedTimeMetric = 0;

        this.hsQualifications = new HashSet<>(20);
        this.liAssignedBlocks = new ArrayList<>(10);

        this.mAvailability = new HashMap<>(10);
        this.mAvailability.put("U", new ArrayList<>());
        this.mAvailability.put("M", new ArrayList<>());
        this.mAvailability.put("T", new ArrayList<>());
        this.mAvailability.put("W", new ArrayList<>());
        this.mAvailability.put("R", new ArrayList<>());
        this.mAvailability.put("F", new ArrayList<>());
        this.mAvailability.put("S", new ArrayList<>());
    }

    private void logAssignment(TimeBlock tb) {
        sbOutputHandler.setLength(0);
        sbOutputHandler.append(sLastName)
        .append(", ")
        .append(sFirstName)
        .append(" [@id = ")
        .append(iId)
        .append("; @assigned = ")
        .append(dblAssignedTimeMetric)
        .append(" / ")
        .append(dblRequestedTimeMetric)
        .append("]")
        .append(" has been assigned ")
        .append(tb.getLabel())
        .append(" on ")
        .append(tb.getDay())
        .append(" [")
        .append(Common.prettyTime(tb.getInterval().getStart()))
        .append(" - ")
        .append(Common.prettyTime(tb.getInterval().getEnd()))
        .append("]");

        logger.atLevel(Level.getLevel("ASSIGN")).log(sbOutputHandler.toString());
    }

    public int getId() {
        return iId;
    }

    public Employee setPriority(int priority) {
        this.iPriority = priority;
        return this;
    }

    public int getPriority() {
        return iPriority;
    }

    public Employee setRequestedTimeMetric(double requestedTimeMetric) {
        this.dblRequestedTimeMetric = requestedTimeMetric;
        return this;
    }

    public double getRequestedTimeMetric() {
        return this.dblRequestedTimeMetric;
    }

    public double getAssignedTimeMetric() {
        return this.dblAssignedTimeMetric;
    }

    public Employee setLastName(String last) {
        this.sLastName = last;
        return this;
    }

    public String getLastName() {
        return sLastName;
    }

    public Employee setFirstName(String first) {
        this.sFirstName = first;
        return this;
    }

    public String getFirstName() {
        return sFirstName;
    }

    public Employee addQualification(String qualification) {
        this.hsQualifications.add(qualification);
        return this;
    }

    public Iterator<String> getQualifications() {
        return this.hsQualifications.iterator();
    }

    public void assignTimeBlock(TimeBlock tb) {
        tb.setAssignedEmployee(this);
        this.liAssignedBlocks.add(tb);
        this.dblAssignedTimeMetric += tb.getTimeMetric();
        logAssignment(tb);
    }

    public void assignTimeBlocks(List<TimeBlock> timeBlocks) {
        timeBlocks.stream().forEach(tb -> {
            tb.setAssignedEmployee(this);
            this.liAssignedBlocks.add(tb);
            this.dblAssignedTimeMetric += tb.getTimeMetric();
            logAssignment(tb);
        });
    }

    public List<TimeBlock> getAssignedTimeBlocks() {
        return liAssignedBlocks;
    }

    public boolean hasRemainingTime(TimeBlock tb) {
        return (tb.getTimeMetric() + this.dblAssignedTimeMetric) <= this.dblRequestedTimeMetric;
    }

    public void addAvailability(String day, Availability avbl) {
        this.mAvailability.get(day).add(avbl);
    }

    public Map<String, List<Availability>> getAvailability() {
        return this.mAvailability;
    }

    public boolean isAvailableFor(TimeBlock tb) {
        List<Availability> liDayAvailability = mAvailability.get(tb.getDay());

        for (Availability avAvailItem : liDayAvailability) {
            if (avAvailItem.contains(tb.getInterval())) {
                return true;
            }
        }

        return false;
    }

    public boolean hasTimeConflict(TimeBlock tbQuery) {
        TimeInterval tiAssigned = null;
        TimeInterval tiQuery = tbQuery.getInterval();

        for (TimeBlock tbAssigned : liAssignedBlocks) {
            if (tbAssigned.getDay().equalsIgnoreCase(tbQuery.getDay())) {
                tiAssigned = tbAssigned.getInterval();
                
                if (tiAssigned.contains(tiQuery)) {
                    return true;
                }
            }
        }

        return false;
    }

    public void clearAssignments() {
        liAssignedBlocks.clear();
        dblAssignedTimeMetric = 0;
    }

    @Override
    public int compareTo(Employee o) {
        return this.iPriority - o.getPriority();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (!(o instanceof Employee)) {
            return false;
        }

        return this.compareTo((Employee) o) == 0;
    }

    @Override
    public int hashCode() {
        return this.iPriority;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(sLastName)
        .append(", ")
        .append(sFirstName)
        .append(" [@id = ")
        .append(iId)
        .append("; @priority = ")
        .append(iPriority)
        .append("; @requested_time_metric = ")
        .append(String.format("%.2f", dblRequestedTimeMetric))
        .append("; @assigned_time_metric = ")
        .append(String.format("%.2f", dblAssignedTimeMetric))
        .append("; @qualifications = ")
        .append(hsQualifications.stream().collect(Collectors.joining(",", "{", "}")))
        .append("]");

        return sb.toString();
    }
}

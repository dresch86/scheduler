/*
 * Scheduler is a tool for assigning schedules to employees with 
 * constraints.
 * 
 * Copyright (C) 2023  Daniel J. Resch, Ph.D.
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.ose.scheduler.controllers;

import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Spliterator;
import java.util.Spliterators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import java.util.stream.Stream;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.time.LocalTime;
import javafx.scene.control.TableView;

import org.dhatim.fastexcel.Workbook;
import org.dhatim.fastexcel.Worksheet;
import org.dhatim.fastexcel.reader.Row;
import org.dhatim.fastexcel.reader.Sheet;
import org.dhatim.fastexcel.reader.ReadableWorkbook;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import org.apache.commons.lang3.SystemUtils;

import org.ose.scheduler.Common;

import org.ose.scheduler.data.Employee;
import org.ose.scheduler.data.TimeBlock;
import org.ose.scheduler.data.Availability;

import org.ose.scheduler.structures.TimeInterval;
import org.ose.scheduler.structures.IntervalTree;
import org.ose.scheduler.structures.AVLIntervalTree;

public class DataController {
    private File fInputFile;
    private File fOutputFile;

    private boolean boolMultiAssign;
    private boolean boolQualTallyReport;
    private boolean boolMetricSummaryReport;

    private TableView<TimeBlock> tvAssignmentsTable;

    private final Pattern pTimeFormat;
    private final Map<String, Integer> hmQualCounts;
    private final Map<Integer, Employee> hmEmployees;
    private final Map<Integer, TimeBlock> hmTimeBlocksMap;
    private final Map<String, List<Integer>> hmTimeBlocksSorted;
    private final Map<Integer, List<Integer>> hmTimeBlockPairing;
    private final List<Map.Entry<String, Integer>> liSortedQualCounts;
    private final Map<String, Map<String, IntervalTree<TimeBlock>>> hmTimeBlockTree;
    private final Map<String, Map<String, IntervalTree<Employee>>> hmEmplAvailability;

    private static final Logger logger = LogManager.getLogger(DataController.class);

    public DataController() {
        pTimeFormat = Pattern.compile("^(\\d{1,2}):(\\d{1,2})([A-Za-z]{2})$");

        boolMultiAssign = false;
        boolQualTallyReport = false;
        boolMetricSummaryReport = false;

        hmEmployees = new HashMap<>(50);
        hmQualCounts = new HashMap<>(25);
        hmTimeBlocksMap = new HashMap<>(150);
        hmTimeBlockPairing = new HashMap<>(50);
        hmTimeBlocksSorted = new HashMap<>(50);
        liSortedQualCounts = new ArrayList<>(25);

        hmEmplAvailability = new HashMap<>(7);
        hmEmplAvailability.put("U", new HashMap<>());
        hmEmplAvailability.put("M", new HashMap<>());
        hmEmplAvailability.put("T", new HashMap<>());
        hmEmplAvailability.put("W", new HashMap<>());
        hmEmplAvailability.put("R", new HashMap<>());
        hmEmplAvailability.put("F", new HashMap<>());
        hmEmplAvailability.put("S", new HashMap<>());

        hmTimeBlockTree = new HashMap<>(7);
        hmTimeBlockTree.put("U", new HashMap<>());
        hmTimeBlockTree.put("M", new HashMap<>());
        hmTimeBlockTree.put("T", new HashMap<>());
        hmTimeBlockTree.put("W", new HashMap<>());
        hmTimeBlockTree.put("R", new HashMap<>());
        hmTimeBlockTree.put("F", new HashMap<>());
        hmTimeBlockTree.put("S", new HashMap<>());
    }

    private LocalTime toLocalTime(String time) {
        Matcher mtResult = pTimeFormat.matcher(time);

        if (mtResult.find()) {
            int iHour = Integer.parseInt(mtResult.group(1));
            int iMinute = Integer.parseInt(mtResult.group(2));

            if (mtResult.group(3).equalsIgnoreCase("PM") && (iHour < 12)) {
                iHour += 12;
            }

            return LocalTime.of(iHour, iMinute);
        } else {
            return null;
        }
    }

    private List<Map.Entry<String, Integer>> sortQualsByTally(Employee empl) {
        Map<String, Integer> hmEmplQuals = new HashMap<>();
        Stream<String> stmQualStream = StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                        empl.getQualifications(),
                        Spliterator.ORDERED), true);
        stmQualStream.forEach(qualCode -> hmEmplQuals.put(qualCode, hmQualCounts.get(qualCode)));

        // Sort matching quals by tally to attempt assignments to blocks with fewest qualified employees first
        List<Map.Entry<String, Integer>> liRelevantQualSortedByTally = new ArrayList<>(hmEmplQuals.entrySet());
        liRelevantQualSortedByTally.sort(Map.Entry.comparingByValue());

        return liRelevantQualSortedByTally;
    }

    private void processQualificationSheet(Sheet qualifications) {
        logger.info("Processing qualifications...");

        try (Stream<Row> rows = qualifications.openStream()) {
            // Skip over headings
            rows.skip(1).forEach(row -> {
                // Process qualifications data

                Optional<String> opstQual = row.getCellAsString(0);
                if (opstQual.isPresent()) {
                    hmQualCounts.put(opstQual.get(), Integer.valueOf(0));
                }
            });

            logger.info("Finished processing qualifications...");
        } catch (IOException ioe) {
            logger.fatal(ioe.getMessage());
        }
    }

    private void processWorkforceSheet(Sheet workforce) {
        logger.info("Processing workforce...");
        StringBuilder sbOutput = new StringBuilder();

        try (Stream<Row> rows = workforce.openStream()) {
            // Skip over headings
            rows.skip(1).forEach(row -> {
                // Process workforce data

                Employee emWorker = new Employee(Integer.parseInt(row.getCellText(0)))
                .setPriority(Integer.parseInt(row.getCellText(1)))
                .setRequestedTimeMetric(Integer.parseInt(row.getCellText(2)))
                .setFirstName(row.getCellText(3))
                .setLastName(row.getCellText(4));

                // Register qualifications
                Arrays.stream(row.getCellText(5).split(","))
                .map(str -> str.trim().toUpperCase())
                .forEach(str -> {
                    emWorker.addQualification(str);

                    if (hmQualCounts.containsKey(str)) {
                        hmQualCounts.put(str, Integer.valueOf(hmQualCounts.get(str).intValue() + 1));
                    } else {
                        sbOutput.append("Employee [@id = ")
                        .append(emWorker.getId())
                        .append("] contains unregistered qualification [ ")
                        .append(str)
                        .append(" ]\n");
                    }
                });

                hmEmployees.put(Integer.valueOf(emWorker.getId()), emWorker);
            });

            if (!sbOutput.isEmpty()) {
                logger.warn(sbOutput::toString);
            }

            logger.info("Finished processing workforce...");
        } catch (IOException ioe) {
            logger.fatal(ioe.getMessage());
        }
    }

    private void processAvailabilitySheet(Sheet availability) {
        logger.info("Processing workforce availability...");
        StringBuilder sbOutput = new StringBuilder();

        try (Stream<Row> rows = availability.openStream()) {
            // Skip over headings
            rows.skip(1).forEach(row -> {
                // Process availability data

                Integer iUID = Integer.valueOf(row.getCellText(1));
                LocalTime ltEnd = toLocalTime(row.getCellText(4));
                LocalTime ltStart = toLocalTime(row.getCellText(3));

                if (hmEmployees.containsKey(iUID)) {
                    String sDay = row.getCellText(2).toUpperCase();
                    Employee emEmployee = hmEmployees.get(iUID);
                    Iterator<String> itsQualifications = emEmployee.getQualifications();

                    while (itsQualifications.hasNext()) {
                        String sQualCode = itsQualifications.next();

                        if (!hmEmplAvailability.get(sDay).containsKey(sQualCode)) {
                            hmEmplAvailability.get(sDay).put(sQualCode, new AVLIntervalTree<>());
                        }

                        hmEmplAvailability.get(sDay).get(sQualCode).addNode(new TimeInterval(ltStart, ltEnd), emEmployee);
                    }

                    emEmployee.addAvailability(sDay, new Availability(ltStart, ltEnd));
                } else {
                    sbOutput.append("Availability entry [ @id = ")
                    .append(row.getCellText(0))
                    .append("]")
                    .append(" contains unregistered EID [ @eid = ")
                    .append(iUID)
                    .append(" ]\n");
                }
            });

            if (!sbOutput.isEmpty()) {
                logger.error(sbOutput::toString);
            }

            logger.info("Finished processing workforce availability...");
        } catch (IOException ioe) {
            logger.fatal(ioe.getMessage());
        }
    }

    private void processTimeBlocksSheet(Sheet timeBlocks) {
        logger.info("Processing time blocks...");
        StringBuilder sbOutput = new StringBuilder();

        try (Stream<Row> rows = timeBlocks.openStream()) {
            // Skip over headings
            rows.skip(1).forEach(row -> {
                // Process availability data

                int iId = Integer.parseInt(row.getCellText(0));
                int iStatus = Integer.parseInt(row.getCellText(9));
                double dblTimeMetric = Double.parseDouble(row.getCellText(7));

                String sDay = row.getCellText(4);
                sDay = sDay.toUpperCase();

                String sLabel = row.getCellText(1);
                String sEndTime = row.getCellText(6);
                String sStartTime = row.getCellText(5);

                String sQualCode = row.getCellText(2);
                sQualCode = sQualCode.trim();
                sQualCode = sQualCode.toUpperCase();

                LocalTime ltEnd = toLocalTime(sEndTime);
                LocalTime ltStart = toLocalTime(sStartTime);

                TimeBlock tbWorkPeriod = new TimeBlock(iId, sLabel)
                .setLocation(row.getCellText(3))
                .setDayAndTime(sDay, ltStart, ltEnd)
                .setTimeMetric(dblTimeMetric)
                .setStatus(iStatus)
                .makePrintable();

                hmTimeBlocksMap.put(iId, tbWorkPeriod);

                if (!hmTimeBlocksSorted.containsKey(sQualCode)) {
                    List<Integer> liTimeBlocksByQual = new ArrayList<>(25);
                    liTimeBlocksByQual.add(Integer.valueOf(iId));
                    hmTimeBlocksSorted.put(sQualCode, liTimeBlocksByQual);
                } else {
                    hmTimeBlocksSorted.get(sQualCode).add(Integer.valueOf(iId));
                }

                if (!hmTimeBlockTree.get(sDay).containsKey(sQualCode)) {
                    hmTimeBlockTree.get(sDay).put(sQualCode, new AVLIntervalTree<>());
                    hmTimeBlockTree.get(sDay).get(sQualCode).addNode(tbWorkPeriod.getInterval(), tbWorkPeriod);
                } else {
                    hmTimeBlockTree.get(sDay).get(sQualCode).addNode(tbWorkPeriod.getInterval(), tbWorkPeriod);
                }

                String sSessionCoupling = row.getCellText(8);
                sSessionCoupling = sSessionCoupling.trim();

                if (sSessionCoupling.length() > 0) {
                    List<Integer> liCouplings = Arrays.stream(sSessionCoupling.split(","))
                    .map(str -> Integer.valueOf(str.trim()))
                    .collect(Collectors.toList());

                    // Store the session couplings for object linking after all sessions registered
                    hmTimeBlockPairing.put(iId, liCouplings);
                }

                sbOutput.append("Added time block ")
                .append("[ ")
                .append(sDay)
                .append(", ")
                .append(Common.prettyTime(ltStart))
                .append(", ")
                .append(Common.prettyTime(ltEnd))
                .append(" ]");

                logger.info(sbOutput.toString());
                sbOutput.setLength(0);
            });

            logger.info("Finished processing time blocks...");
        } catch (IOException ioe) {
            logger.fatal(ioe.getMessage());
        }
    }

    private boolean assignedPairedTimeBlocks(final Employee candidateEmployee, 
                                                final TimeBlock currentTimeBlock) {
        List<Integer> liPairedTimeBlockIds = hmTimeBlockPairing.get(Integer.valueOf(currentTimeBlock.getId()));
        int iPairedSessionCount = liPairedTimeBlockIds.size();

        // Use this list to temporarily hold blocks that might be assignable if time metrics 
        // check out for all blocks.
        List<TimeBlock> liTimeBlocksToAssign = new ArrayList<>(10);
        liTimeBlocksToAssign.add(currentTimeBlock);

        double dblAssignedTimeMetric = candidateEmployee.getAssignedTimeMetric();
        boolean boolAssignableToPairedTBs = true;

        for (int k = 0; k < iPairedSessionCount; k++) {
            TimeBlock tbPairedTimeBlock = hmTimeBlocksMap.get(liPairedTimeBlockIds.get(k));
            dblAssignedTimeMetric += tbPairedTimeBlock.getTimeMetric();
            
            if ((dblAssignedTimeMetric <= candidateEmployee.getRequestedTimeMetric()) 
                && candidateEmployee.isAvailableFor(tbPairedTimeBlock)
                && !candidateEmployee.hasTimeConflict(tbPairedTimeBlock)) {
                liTimeBlocksToAssign.add(tbPairedTimeBlock);
            } else {
                // Employee not available for paired session, so break to try someone else
                boolAssignableToPairedTBs = false;
                break;
            }
        }

        if (boolAssignableToPairedTBs) {
            candidateEmployee.assignTimeBlocks(liTimeBlocksToAssign);
            return true;
        } else {
            return false;
        }
    } 

    private void multiAssigner() {
        logger.info("Making multi assignments...");

        // Sort quals by number of folks in each to begin assigning blocks with least number of qualified folks first
        liSortedQualCounts.addAll(hmQualCounts.entrySet());
        liSortedQualCounts.sort(Map.Entry.comparingByValue());

        List<Employee> liEmployeesSortedByPriority = new ArrayList<>(hmEmployees.values());
        Collections.sort(liEmployeesSortedByPriority);

        for (Employee emCurrEmpl : liEmployeesSortedByPriority) {
            Map<String, List<Availability>> hmCurrEmplAvailability = emCurrEmpl.getAvailability();
            List<Map.Entry<String, Integer>> liQualsByCount = sortQualsByTally(emCurrEmpl);

            for (Map.Entry<String, List<Availability>> meAvailabilityByDay : hmCurrEmplAvailability.entrySet()) {
                String sDay = meAvailabilityByDay.getKey();

                for (Availability avAvailabilitySlot : meAvailabilityByDay.getValue()) {
                    
                }
            }
        }

        logger.info("Finished making multi assignments...");
    }

    private void quickAssigner() {
        logger.info("Making quick assignments...");

        // Sort quals by number of folks in each to begin assigning blocks with least number of qualified folks first
        liSortedQualCounts.addAll(hmQualCounts.entrySet());
        liSortedQualCounts.sort(Map.Entry.comparingByValue());

        // Reuse these variables while searching for assignments to make
        int iNumSortedTimeBlocks;
        TimeInterval tiWorkingInterval;
        IntervalTree<Employee> iteEmployees;
        Iterator<Employee> iteEmployeesCursor;
        List<Integer> liTimeBlocksSortedByTimeMetric;

        for (Map.Entry<String, Integer> meQualSortedByEmplCount : liSortedQualCounts) {
            final String sQualCode = meQualSortedByEmplCount.getKey();

            if (meQualSortedByEmplCount.getValue() == 0) {
                // There are no qualified employees so no need to check time blocks
                logger.warn(() -> sQualCode + " has no qualified employees");
            } else {
                // Sort time blocks such that ones with highest time metric assign first
                liTimeBlocksSortedByTimeMetric = hmTimeBlocksSorted.get(sQualCode);
                liTimeBlocksSortedByTimeMetric.sort((o1, o2) -> {
                    // Sort by time metric to favor assignment of longest blocks early on
                    if (hmTimeBlocksMap.get(o1).getTimeMetric() == hmTimeBlocksMap.get(o2).getTimeMetric()) {
                        return 0;
                    } else {
                        return (hmTimeBlocksMap.get(o1).getTimeMetric() > hmTimeBlocksMap.get(o2).getTimeMetric()) ? 1 : -1;
                    }
                });

                iNumSortedTimeBlocks = liTimeBlocksSortedByTimeMetric.size();
                logger.info(() -> sQualCode + " has " + meQualSortedByEmplCount.getValue() + " qualified employees");

                for (int j = 0; j < iNumSortedTimeBlocks; j++) {
                    final TimeBlock tbWorkingBlock = hmTimeBlocksMap.get(liTimeBlocksSortedByTimeMetric.get(j));
                    logger.info(() -> "Analyzing time block " + tbWorkingBlock.toString() + "...");

                    if (tbWorkingBlock.getStatus() == 0) {
                        if (tbWorkingBlock.getAssignedEmployee() == null) {
                            iteEmployees = hmEmplAvailability.get(tbWorkingBlock.getDay()).get(sQualCode);

                            if (iteEmployees != null) {
                                tiWorkingInterval = tbWorkingBlock.getInterval();
                                final SortedSet<Employee> sseAvailableEmpls = iteEmployees.overlaps(tiWorkingInterval);
                                logger.info(() -> "Found " + sseAvailableEmpls.size() + " available employee(s) for " + tbWorkingBlock.toString());

                                boolean boolTimeBlockAssigned = false;
                                iteEmployeesCursor = sseAvailableEmpls.iterator();

                                while (!boolTimeBlockAssigned && iteEmployeesCursor.hasNext()) {
                                    final Employee emplWorkingPerson = iteEmployeesCursor.next();
                                    logger.info(() -> "Querying " + emplWorkingPerson.getLastName() + ", " + emplWorkingPerson.getFirstName() + " for assignment to " + tbWorkingBlock.getLabel());

                                    if (emplWorkingPerson.hasRemainingTime(tbWorkingBlock)) {
                                        if (hmTimeBlockPairing.containsKey(Integer.valueOf(tbWorkingBlock.getId()))) {
                                            boolTimeBlockAssigned = assignedPairedTimeBlocks(emplWorkingPerson, tbWorkingBlock);
                                        } else {
                                            if (!emplWorkingPerson.hasTimeConflict(tbWorkingBlock)) {
                                                emplWorkingPerson.assignTimeBlock(tbWorkingBlock);
                                                boolTimeBlockAssigned = true;
                                            } else {
                                                logger.info(() -> emplWorkingPerson.getLastName() + ", " + emplWorkingPerson.getFirstName() + " has conflict with " + tbWorkingBlock.getLabel());
                                            }
                                        }
                                    } else {
                                        logger.info(() -> emplWorkingPerson.getLastName() + ", " + emplWorkingPerson.getFirstName() + " [@priority = " + emplWorkingPerson.getPriority() + "] has a full schedule");
                                    }
                                }
                            } else {
                                logger.info(() -> "No qualified employees for time block " + tbWorkingBlock.toString());
                            }
                        } else {
                            logger.info(() -> "Time block already assigned " + tbWorkingBlock.toString());
                        }
                    } else {
                        logger.warn(() -> "Time block marked as manually assigned " + tbWorkingBlock.toString());
                    }
                }
            }
        }

        logger.info("Finished making quick assignments...");
    }

    private CompletableFuture<Void> generateTimeMetricSummary(Workbook wb) {
        return CompletableFuture.runAsync(() -> {
            int iEmployeeRowIndex = 1;

            Worksheet wsEmployeeReport = wb.newWorksheet("Employees");
            wsEmployeeReport.value(0, 0, "EID");
            wsEmployeeReport.value(0, 1, "Last");
            wsEmployeeReport.value(0, 2, "First");
            wsEmployeeReport.value(0, 3, "Priority");
            wsEmployeeReport.value(0, 4, "Assigned Time");
            wsEmployeeReport.value(0, 5, "Requested Time");

            for (Employee emplCurrentEmployee : hmEmployees.values()) {
                wsEmployeeReport.value(iEmployeeRowIndex, 0, emplCurrentEmployee.getId());
                wsEmployeeReport.value(iEmployeeRowIndex, 1, emplCurrentEmployee.getLastName());
                wsEmployeeReport.value(iEmployeeRowIndex, 2, emplCurrentEmployee.getFirstName());
                wsEmployeeReport.value(iEmployeeRowIndex, 3, emplCurrentEmployee.getPriority());
                wsEmployeeReport.value(iEmployeeRowIndex, 4, emplCurrentEmployee.getAssignedTimeMetric());
                wsEmployeeReport.value(iEmployeeRowIndex, 5, emplCurrentEmployee.getRequestedTimeMetric());

                iEmployeeRowIndex += 1;
            }
        });
    }

    private CompletableFuture<Void> generateQualTallyReport(Workbook wb) {
         return CompletableFuture.runAsync(() -> {
            int iEmployeeRowIndex = 1;

            Worksheet wsQualTallyReport = wb.newWorksheet("Qualification Tally");
            wsQualTallyReport.value(0, 0, "Qualification Code");
         });
    }

    private void outputQuickAssignments() {
        try (FileOutputStream fosAssignmentFile = new FileOutputStream(fOutputFile);
            Workbook wb = new Workbook(fosAssignmentFile, "Scheduler", "1.0")) {
            List<CompletableFuture<Void>> liSheetFutures = new ArrayList<>();

            CompletableFuture<Void> cfAssignments = CompletableFuture.runAsync(() -> {
                Employee emCurrentEmployee;
                int iAssignmentRowIndex = 1;

                Worksheet wsAssignmentSheet = wb.newWorksheet("Assignments");
                wsAssignmentSheet.value(0, 0, "TID");
                wsAssignmentSheet.value(0, 1, "Label");
                wsAssignmentSheet.value(0, 2, "Last");
                wsAssignmentSheet.value(0, 3, "First");
                wsAssignmentSheet.value(0, 4, "Manually Assigned");
                wsAssignmentSheet.value(0, 5, "Time Metric");
                wsAssignmentSheet.value(0, 6, "Day");
                wsAssignmentSheet.value(0, 7, "Start");
                wsAssignmentSheet.value(0, 8, "End");

                for (TimeBlock tbCurrentTimeBlock : hmTimeBlocksMap.values()) {
                    emCurrentEmployee = tbCurrentTimeBlock.getAssignedEmployee();

                    if (emCurrentEmployee != null) {
                        wsAssignmentSheet.value(iAssignmentRowIndex, 0, String.valueOf(tbCurrentTimeBlock.getId()));
                        wsAssignmentSheet.value(iAssignmentRowIndex, 1, tbCurrentTimeBlock.getLabel());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 2, emCurrentEmployee.getLastName());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 3, emCurrentEmployee.getFirstName());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 4, (tbCurrentTimeBlock.getStatus() == 1) ? "Y" : "N");
                        wsAssignmentSheet.value(iAssignmentRowIndex, 5, tbCurrentTimeBlock.getTimeMetric());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 6, tbCurrentTimeBlock.getDay());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 7, Common.prettyTime(tbCurrentTimeBlock.getInterval().getStart()));
                        wsAssignmentSheet.value(iAssignmentRowIndex, 8, Common.prettyTime(tbCurrentTimeBlock.getInterval().getEnd()));
                    } else {
                        wsAssignmentSheet.value(iAssignmentRowIndex, 0, String.valueOf(tbCurrentTimeBlock.getId()));
                        wsAssignmentSheet.value(iAssignmentRowIndex, 1, tbCurrentTimeBlock.getLabel());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 2, "--");
                        wsAssignmentSheet.value(iAssignmentRowIndex, 3, "--");
                        wsAssignmentSheet.value(iAssignmentRowIndex, 4, (tbCurrentTimeBlock.getStatus() == 1) ? "Y" : "N");
                        wsAssignmentSheet.value(iAssignmentRowIndex, 5, tbCurrentTimeBlock.getTimeMetric());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 6, tbCurrentTimeBlock.getDay());
                        wsAssignmentSheet.value(iAssignmentRowIndex, 7, Common.prettyTime(tbCurrentTimeBlock.getInterval().getStart()));
                        wsAssignmentSheet.value(iAssignmentRowIndex, 8, Common.prettyTime(tbCurrentTimeBlock.getInterval().getEnd()));
                    }

                    iAssignmentRowIndex += 1;
                }
            });

            liSheetFutures.add(cfAssignments);

            if (boolMetricSummaryReport) {
                liSheetFutures.add(generateTimeMetricSummary(wb));
            }

            if (boolQualTallyReport) {
                liSheetFutures.add(generateQualTallyReport(wb));
            }

            CompletableFuture.allOf(liSheetFutures.toArray(new CompletableFuture[liSheetFutures.size()])).get();
        } catch (IOException ioe) {
            logger.fatal(ioe);
        } catch(InterruptedException ie) {
            logger.error(ie);
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            logger.error(e);
        }
    }

    public void outputMultiAssignments() {
        // TODO Output reports for all possible schedules
    }

    public void parseInput() {
        long lStartTime = System.currentTimeMillis();

        try (FileInputStream fis = new FileInputStream(fInputFile); 
            ReadableWorkbook rwbInputBook = new ReadableWorkbook(fis)) {
            Optional<Sheet> opshQualList = rwbInputBook.findSheet("Qualifications");

            if (opshQualList.isPresent()) {
                processQualificationSheet(opshQualList.get());
            } else {
                logger.error("Missing qualifications sheet!");
            }

            Optional<Sheet> opshWorkforceList = rwbInputBook.findSheet("Workforce");

            if (opshWorkforceList.isPresent()) {
                processWorkforceSheet(opshWorkforceList.get());
            } else {
                logger.error("Missing workforce sheet!");
            }

            Optional<Sheet> opshAvailabilityList = rwbInputBook.findSheet("Availability");

            if (opshAvailabilityList.isPresent()) {
                processAvailabilitySheet(opshAvailabilityList.get());
            } else {
                logger.error("Missing availability sheet!");
            }

            Optional<Sheet> opshTimeBlocksList = rwbInputBook.findSheet("Time Blocks");

            if (opshTimeBlocksList.isPresent()) {
                processTimeBlocksSheet(opshTimeBlocksList.get());
            } else {
                logger.error("Missing time blocks sheet!");
            }

            if (boolMultiAssign) {
                multiAssigner();
                outputMultiAssignments();
            } else {
                quickAssigner();
                outputQuickAssignments();
            }
        } catch (IOException ioe) {
            logger.fatal(ioe.getMessage());
        }

        long lFinishTime = System.currentTimeMillis();
        logger.info(() -> "Time elapsed: " + (lFinishTime - lStartTime) + " ms");
    }

    public void openReport(Consumer<String> cb) {
        String[] sCmd;

        if (SystemUtils.IS_OS_WINDOWS) {
            sCmd = new String[] {"cmd", "/c", "call", ("\"" + fOutputFile.getAbsolutePath() + "\"")};
        } else if (SystemUtils.IS_OS_MAC) {
            sCmd = new String[] {"open", ("\"" + fOutputFile.getAbsolutePath() + "\"")};
        } else if (SystemUtils.IS_OS_LINUX) {
            sCmd =  new String[] {"xdg-open", ("\"" + fOutputFile.getAbsolutePath() + "\"")};
        } else {
            cb.accept("Unsupported OS!");
            return;
        }

        try {
            ProcessBuilder pbExecExcel = new ProcessBuilder(sCmd);
            pbExecExcel.redirectErrorStream(true);
            pbExecExcel.start();
        } catch (IOException ioe) {
            cb.accept("A filesystem error occured in opening the assignment report!");
            logger.fatal(ioe::getMessage);
        }
    }

    public void setOutputFile(File fileOut) {
        fOutputFile = fileOut;
    }

    public void setInputFile(File fileIn) {
        fInputFile = fileIn;
    }

    public boolean hasInputFileSet() {
        return fInputFile != null;
    }

    public boolean hasOutputFileSet() {
        return fOutputFile != null;
    }

    public void setMultiAssignMode(boolean mode) {
        boolMultiAssign = mode;
    }

    public void setQualTallyReport(boolean tallyReport) {
        boolQualTallyReport = tallyReport;
    }

    public void setMetricSummaryReport(boolean metricSummaryReport) {
        boolMetricSummaryReport = metricSummaryReport;
    }

    public void setDisplayTable(TableView<TimeBlock> timeBlockTable) {
        tvAssignmentsTable = timeBlockTable;
    }

    public void cleanup() {
        for (Employee empl : hmEmployees.values()) {
            empl.clearAssignments();
        }

        for (TimeBlock tb : hmTimeBlocksMap.values()) {
            tb.setAssignedEmployee(null);
        }
    }
}
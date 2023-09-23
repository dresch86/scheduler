package org.ose.scheduler.structures;

import java.util.Set;
import java.util.TreeSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.SortedSet;

import java.time.LocalTime;

import org.ose.scheduler.Common;

public class AVLIntervalTree<T extends Comparable<T>> implements IntervalTree<T> {
    private int iNumNodes;
    private AVLIntervalNode<T> root;

    public AVLIntervalTree() {
        root = null;
        iNumNodes = 0;
    }

    private int height(AVLIntervalNode<T> node) {
        if (node == null) {
            return 0;
        } else {
            return node.getHeight();
        }
    }

    private AVLIntervalNode<T> leftRotate(AVLIntervalNode<T> parentBefore) {
        AVLIntervalNode<T> parentAfter = parentBefore.getRight();
        parentBefore.setRight(parentAfter.getLeft());
        parentAfter.setLeft(parentBefore);
 
        // Update heights
        parentBefore.setHeight(Math.max(height(parentBefore.getRight()), height(parentBefore.getLeft())) + 1);
        parentAfter.setHeight(Math.max(height(parentAfter.getRight()), height(parentAfter)) + 1);

        return parentAfter;
    }

    private AVLIntervalNode<T> rightRotate(AVLIntervalNode<T> parentBefore) {
        AVLIntervalNode<T> parentAfter = parentBefore.getLeft();
        parentBefore.setLeft(parentAfter.getRight());
        parentAfter.setRight(parentBefore);
 
        // Update heights
        parentBefore.setHeight(Math.max(height(parentBefore.getRight()), height(parentBefore.getLeft())) + 1);
        parentAfter.setHeight(Math.max(height(parentAfter.getLeft()), height(parentBefore)) + 1);

        return parentAfter;
    }

    private AVLIntervalNode<T> rebalance(AVLIntervalNode<T> workingNode, AVLIntervalNode<T> addedNode) {
        int iBalanceFactor = workingNode.getBalanceFactor();

        if (iBalanceFactor > 1) {
            int iCompareRes = addedNode.compareTo(workingNode.getLeft());

            if (iCompareRes < 0) {
                return rightRotate(workingNode);
            } else {
                AVLIntervalNode<T> intRotated = rightRotate(workingNode);
                return leftRotate(intRotated);
            }
        } else if (iBalanceFactor < -1) {
            int iCompareRes = addedNode.compareTo(workingNode.getRight());

            if (iCompareRes > 0) {
                return leftRotate(workingNode);
            } else {
                AVLIntervalNode<T> intRotated = leftRotate(workingNode);
                return rightRotate(intRotated);
            }
        } else {
            return workingNode;
        }
    }

    private AVLIntervalNode<T> recurse(AVLIntervalNode<T> headNode, AVLIntervalNode<T> submittedNode) {
        int iCompareResult = submittedNode.compareTo(headNode);

        if (iCompareResult > 0) {
            // Go right

            if (headNode.getRight() != null) {
                headNode.setRight(recurse(headNode.getRight(), submittedNode));
            } else {
                headNode.setRight(submittedNode);
                iNumNodes += 1;
            }

            AVLIntervalNode<T> itnHeadNode = rebalance(headNode, submittedNode);
            itnHeadNode.setHeight(Math.max(height(itnHeadNode.getLeft()), height(itnHeadNode.getRight())) + 1);
            itnHeadNode.updateMinMax();

            return itnHeadNode;
        } else if (iCompareResult < 0) {
            // Go left

            if (headNode.getLeft() != null) {
                headNode.setLeft(recurse(headNode.getLeft(), submittedNode));
            } else {
                headNode.setLeft(submittedNode);
                iNumNodes += 1;
            }

            AVLIntervalNode<T> itnHeadNode = rebalance(headNode, submittedNode);
            itnHeadNode.setHeight(Math.max(height(itnHeadNode.getLeft()), height(itnHeadNode.getRight())) + 1);
            itnHeadNode.updateMinMax();

            return itnHeadNode;
        } else {
            // Don't add a new node since it is a duplicate interval, but append data
            Iterator<T> itDataToCopy = submittedNode.getDataIterator();

            while (itDataToCopy.hasNext()) {
                headNode.addData(itDataToCopy.next());
            }
            
            return headNode;
        }
    }

    private void overlaps(AVLIntervalNode<T> headNode, LocalTime lower, LocalTime upper, SortedSet<T> result) {
        if (headNode != null) {
            int iLowerResult = lower.compareTo(headNode.getLowerBound());
            int iUpperResult = upper.compareTo(headNode.getUpperBound());

            if ((iLowerResult >= 0) && (iUpperResult <= 0)) {
                headNode.getDataIterator().forEachRemaining(result::add);
            }

            if (headNode.getLeft() != null) {
                overlaps(headNode.getLeft(), lower, upper, result);
            }

            if (headNode.getRight() != null) {
                overlaps(headNode.getRight(), lower, upper, result);
            }
        }
    }

    private void printNodes(AVLIntervalNode<T> headNode, StringBuilder output) {
        if (headNode == null) {
            output.append(" (null, null) ");
        } else {
            output.append("(")
            .append(Common.prettyTime(headNode.getLowerBound()))
            .append(", ")
            .append(Common.prettyTime(headNode.getUpperBound()))
            .append(") => [|L");

            printNodes(headNode.getLeft(), output);
            output.append(" L|; R| ");
            printNodes(headNode.getRight(), output);
            output.append(" R|]");
        }
    }

    public SortedSet<T> overlaps(TimeInterval timePeriod) {
        if (this.root != null) {
            SortedSet<T> tsResult = new TreeSet<>(Collections.reverseOrder());
            overlaps(this.root, timePeriod.getStart(), timePeriod.getEnd(), tsResult);

            return tsResult;
        } else {
            return new TreeSet<>();
        }
    }

    public IntervalNode<T> exists(TimeInterval timePeriod) {
        if (this.root != null) {
            int iLowerResult;
            int iUpperResult;
            AVLIntervalNode<T> itnCurrentNode = this.root;

            do {
                iLowerResult = timePeriod.getStart().compareTo(itnCurrentNode.getLowerBound());

                if (iLowerResult == 0) {
                    iUpperResult = timePeriod.getEnd().compareTo(itnCurrentNode.getUpperBound());

                    if (iUpperResult == 0) {
                        return itnCurrentNode;
                    } else if (iUpperResult > 0) {
                        itnCurrentNode = itnCurrentNode.getRight();
                    } else {
                        itnCurrentNode = itnCurrentNode.getLeft();
                    }
                } else if (iLowerResult > 0) {
                    itnCurrentNode = itnCurrentNode.getRight();
                } else {
                    itnCurrentNode = itnCurrentNode.getLeft();
                }
            } while (itnCurrentNode != null);

            return null;
        } else {
            return null;
        }
    }

    @Override
    public void addNode(TimeInterval timePeriod, T data) {
        if (this.root == null) {
            this.root = new AVLIntervalNode<>(timePeriod);
            this.root.addData(data);
            iNumNodes += 1;
        } else {
            AVLIntervalNode<T> itnNode = new AVLIntervalNode<>(timePeriod);
            itnNode.addData(data);
            this.root = recurse(this.root, itnNode);
        }
    }

    @Override
    public void addNode(TimeInterval timePeriod, Set<T> data) {
        if (this.root == null) {
            this.root = new AVLIntervalNode<>(timePeriod);
            this.root.addData(data);
            iNumNodes += 1;
        } else {
            AVLIntervalNode<T> itnNode = new AVLIntervalNode<>(timePeriod);
            itnNode.addData(data);
            this.root = recurse(this.root, itnNode);
        }
    }

    public AVLIntervalNode<T> getRootNode() {
        return this.root;
    }

    public int count() {
        return iNumNodes;
    }

    @Override
    public String toString() {
        StringBuilder sbOutput = new StringBuilder();

        if (root != null) {
            printNodes(root, sbOutput);
        } else {
            sbOutput.append("No nodes present");
        }

        return sbOutput.toString();
    }
}
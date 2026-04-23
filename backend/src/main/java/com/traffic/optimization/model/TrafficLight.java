package com.traffic.optimization.model;

import lombok.Data;

/**
 * Traffic light class - supports multiple phases, all-red intervals, and configurable cycles
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@Data
public class TrafficLight {
    /**
     * Traffic light ID (corresponds to node ID)
     */
    private String id;

    /**
     * Current signal direction (East-West / North-South)
     */
    private SignalDirection currentDirection;

    /**
     * Current signal state (RED / GREEN / YELLOW / ALL_RED)
     */
    private SignalState currentState;

    /**
     * Green light duration (seconds) - can be dynamically adjusted by optimization algorithms.
     * Note: when both directions share this value it represents symmetric timing;
     * Webster uses greenDurationEW/NS for asymmetric allocation.
     */
    private int greenDuration;

    /**
     * East-West green light duration (seconds) - for asymmetric timing
     */
    private int greenDurationEW;

    /**
     * North-South green light duration (seconds) - for asymmetric timing
     */
    private int greenDurationNS;

    /**
     * Yellow light duration (seconds)
     */
    private int yellowDuration;

    /**
     * Red light duration (seconds)
     */
    private int redDuration;

    /**
     * All-red interval duration (seconds) - safety clearance interval when all directions are red
     */
    private int allRedDuration;

    /**
     * Remaining time in the current state (seconds)
     */
    private int remainingTime;

    /**
     * Signal offset (seconds) - used for green wave coordination
     */
    private int offset;

    /**
     * Cumulative number of vehicles served (for efficiency analysis)
     */
    private long totalServedVehicles;

    /**
     * Constructor - default configuration
     */
    public TrafficLight(String id) {
        this(id, 30, 3, 2);
    }

    /**
     * Constructor - custom configuration
     *
     * @param id             traffic light ID
     * @param greenDuration  green light duration (seconds)
     * @param yellowDuration yellow light duration (seconds)
     * @param allRedDuration all-red interval duration (seconds)
     */
    public TrafficLight(String id, int greenDuration, int yellowDuration, int allRedDuration) {
        this.id = id;
        this.currentDirection = SignalDirection.EAST_WEST;
        this.currentState = SignalState.GREEN;
        this.greenDuration = greenDuration;
        this.greenDurationEW = greenDuration;
        this.greenDurationNS = greenDuration;
        this.yellowDuration = yellowDuration;
        this.allRedDuration = allRedDuration;
        // Red duration = opposing green + yellow + all-red
        this.redDuration = greenDuration + yellowDuration + allRedDuration;
        this.remainingTime = greenDuration;
        this.offset = 0;
        this.totalServedVehicles = 0;
    }

    /**
     * Update the traffic light state (called every second)
     */
    public void update() {
        remainingTime--;

        if (remainingTime <= 0) {
            switchState();
        }
    }

    /**
     * Switch the signal state.
     * State machine: GREEN -> YELLOW -> ALL_RED -> (switch direction) GREEN -> YELLOW -> ALL_RED -> ...
     *
     * One full cycle = EW(green + yellow + all-red) + NS(green + yellow + all-red)
     * There is no extra RED transition: after ALL_RED ends the direction switches directly to the opposing GREEN.
     */
    private void switchState() {
        switch (currentState) {
            case GREEN:
                currentState = SignalState.YELLOW;
                remainingTime = yellowDuration;
                break;
            case YELLOW:
                if (allRedDuration > 0) {
                    currentState = SignalState.ALL_RED;
                    remainingTime = allRedDuration;
                } else {
                    // No all-red interval - switch directly to opposing GREEN
                    switchDirection();
                    currentState = SignalState.GREEN;
                    remainingTime = greenForDirection(currentDirection);
                }
                break;
            case ALL_RED:
                switchDirection();
                currentState = SignalState.GREEN;
                remainingTime = greenForDirection(currentDirection);
                break;
            case RED:
                // This state can only be entered via a manual synchronize() call; fall back to GREEN as a safety measure
                currentState = SignalState.GREEN;
                remainingTime = greenForDirection(currentDirection);
                break;
        }
    }

    private int greenForDirection(SignalDirection dir) {
        return dir == SignalDirection.EAST_WEST ? greenDurationEW : greenDurationNS;
    }

    private SignalDirection oppositeDirection(SignalDirection dir) {
        return dir == SignalDirection.EAST_WEST
            ? SignalDirection.NORTH_SOUTH
            : SignalDirection.EAST_WEST;
    }

    /**
     * Switch the active direction
     */
    private void switchDirection() {
        currentDirection = (currentDirection == SignalDirection.EAST_WEST)
            ? SignalDirection.NORTH_SOUTH
            : SignalDirection.EAST_WEST;
    }

    /**
     * Check whether traffic in the specified direction may proceed.
     * Both GREEN and YELLOW allow passage (simulating real-world yellow-light behaviour).
     */
    public boolean canPass(SignalDirection direction) {
        if (currentDirection != direction) {
            return false;
        }
        return currentState == SignalState.GREEN || currentState == SignalState.YELLOW;
    }

    /**
     * Adjust green duration symmetrically (both directions use the same value).
     * Used for FIXED timing and other strategies that do not differentiate by direction.
     */
    public void adjustGreenDuration(int newDuration) {
        int clamped = Math.max(10, Math.min(90, newDuration));
        this.greenDuration = clamped;
        this.greenDurationEW = clamped;
        this.greenDurationNS = clamped;
        this.redDuration = greenDuration + yellowDuration + allRedDuration;
    }

    /**
     * Asymmetrically adjust green durations for each direction.
     * Used by Webster to allocate green time according to directional demand.
     */
    public void adjustGreenDurations(int ewDuration, int nsDuration) {
        this.greenDurationEW = Math.max(10, Math.min(90, ewDuration));
        this.greenDurationNS = Math.max(10, Math.min(90, nsDuration));
        this.greenDuration = (greenDurationEW + greenDurationNS) / 2;
        // redDuration is no longer a fixed value (it changes as the direction switches);
        // the field is retained for legacy compatibility but its value is only approximate.
        this.redDuration = greenDuration + yellowDuration + allRedDuration;
    }

    /**
     * Get the full cycle length (seconds).
     * One full cycle = EW green + yellow + all-red + NS green + yellow + all-red
     */
    public int getCycleLength() {
        return greenDurationEW + greenDurationNS + (yellowDuration + allRedDuration) * 2;
    }

    /**
     * Align the signal phase to the given offset (seconds) in one shot - used for green wave coordination.
     *
     * Phase timeline (origin = start of EW green):
     *   [0, ewG)                       EW GREEN
     *   [ewG, ewG+y)                   EW YELLOW
     *   [ewG+y, ewG+y+r)               ALL_RED  (EW -> NS transition)
     *   [ewG+y+r, ewG+y+r+nsG)         NS GREEN
     *   [ewG+y+r+nsG, ewG+y+r+nsG+y)   NS YELLOW
     *   [...+y, cycle)                 ALL_RED  (NS -> EW transition)
     */
    public void synchronize(int offsetSec) {
        int cycle = getCycleLength();
        int t = ((offsetSec % cycle) + cycle) % cycle;
        int ewG = greenDurationEW, nsG = greenDurationNS;
        int y = yellowDuration, r = allRedDuration;

        if (t < ewG) {
            currentDirection = SignalDirection.EAST_WEST;
            currentState = SignalState.GREEN;
            remainingTime = ewG - t;
        } else if (t < ewG + y) {
            currentDirection = SignalDirection.EAST_WEST;
            currentState = SignalState.YELLOW;
            remainingTime = (ewG + y) - t;
        } else if (t < ewG + y + r) {
            currentDirection = SignalDirection.EAST_WEST;
            currentState = SignalState.ALL_RED;
            remainingTime = (ewG + y + r) - t;
        } else if (t < ewG + y + r + nsG) {
            currentDirection = SignalDirection.NORTH_SOUTH;
            currentState = SignalState.GREEN;
            remainingTime = (ewG + y + r + nsG) - t;
        } else if (t < ewG + y + r + nsG + y) {
            currentDirection = SignalDirection.NORTH_SOUTH;
            currentState = SignalState.YELLOW;
            remainingTime = (ewG + y + r + nsG + y) - t;
        } else {
            currentDirection = SignalDirection.NORTH_SOUTH;
            currentState = SignalState.ALL_RED;
            remainingTime = cycle - t;
        }
        this.offset = offsetSec;
    }

    /**
     * Record the number of vehicles served
     */
    public void recordServedVehicles(int count) {
        this.totalServedVehicles += count;
    }

    /**
     * Signal direction enumeration
     */
    public enum SignalDirection {
        EAST_WEST,   // East-West direction
        NORTH_SOUTH  // North-South direction
    }

    /**
     * Signal state enumeration
     */
    public enum SignalState {
        RED,     // Red light
        YELLOW,  // Yellow light
        GREEN,   // Green light
        ALL_RED  // All-red (safety clearance interval)
    }

    @Override
    public String toString() {
        return "TrafficLight{" +
                "id='" + id + '\'' +
                ", direction=" + currentDirection +
                ", state=" + currentState +
                ", remaining=" + remainingTime + "s" +
                ", cycle=" + getCycleLength() + "s" +
                '}';
    }
}

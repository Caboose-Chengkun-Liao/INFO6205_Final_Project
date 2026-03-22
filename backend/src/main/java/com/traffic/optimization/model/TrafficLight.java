package com.traffic.optimization.model;

import lombok.Data;

/**
 * Traffic light class
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
     * Current signal direction (east-west / north-south)
     */
    private SignalDirection currentDirection;

    /**
     * Current signal state (red / green / yellow)
     */
    private SignalState currentState;

    /**
     * Green light duration (seconds)
     */
    private int greenDuration;

    /**
     * Yellow light duration (seconds)
     */
    private int yellowDuration;

    /**
     * Red light duration (seconds)
     */
    private int redDuration;

    /**
     * Remaining time for current state (seconds)
     */
    private int remainingTime;

    /**
     * Constructor - default configuration
     */
    public TrafficLight(String id) {
        this.id = id;
        this.currentDirection = SignalDirection.EAST_WEST;
        this.currentState = SignalState.GREEN;
        this.greenDuration = 30;
        this.yellowDuration = 3;
        this.redDuration = 33; // green + yellow time for the other direction
        this.remainingTime = greenDuration;
    }

    /**
     * Update traffic light state (called each second)
     */
    public void update() {
        remainingTime--;

        if (remainingTime <= 0) {
            switchState();
        }
    }

    /**
     * Switch signal state
     */
    private void switchState() {
        switch (currentState) {
            case GREEN:
                currentState = SignalState.YELLOW;
                remainingTime = yellowDuration;
                break;
            case YELLOW:
                currentState = SignalState.RED;
                remainingTime = redDuration;
                // switch direction
                currentDirection = (currentDirection == SignalDirection.EAST_WEST)
                    ? SignalDirection.NORTH_SOUTH
                    : SignalDirection.EAST_WEST;
                break;
            case RED:
                currentState = SignalState.GREEN;
                remainingTime = greenDuration;
                break;
        }
    }

    /**
     * Check if the specified direction can pass
     */
    public boolean canPass(SignalDirection direction) {
        return currentDirection == direction && currentState == SignalState.GREEN;
    }

    /**
     * Adjust green light duration (for optimization)
     */
    public void adjustGreenDuration(int newDuration) {
        this.greenDuration = Math.max(10, Math.min(60, newDuration)); // limit to 10-60 seconds
        this.redDuration = greenDuration + yellowDuration;
    }

    /**
     * Signal direction enumeration
     */
    public enum SignalDirection {
        EAST_WEST,  // east-west direction
        NORTH_SOUTH // north-south direction
    }

    /**
     * Signal state enumeration
     */
    public enum SignalState {
        RED,    // red light
        YELLOW, // yellow light
        GREEN   // green light
    }

    @Override
    public String toString() {
        return "TrafficLight{" +
                "id='" + id + '\'' +
                ", direction=" + currentDirection +
                ", state=" + currentState +
                ", remaining=" + remainingTime + "s" +
                '}';
    }
}

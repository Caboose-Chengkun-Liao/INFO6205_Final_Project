package com.traffic.optimization.model;

/**
 * Node type enumeration
 */
public enum NodeType {
    /**
     * Internal intersection - controlled by traffic lights
     */
    INTERSECTION,

    /**
     * Boundary entry/exit - start and end points of traffic flows
     */
    BOUNDARY
}

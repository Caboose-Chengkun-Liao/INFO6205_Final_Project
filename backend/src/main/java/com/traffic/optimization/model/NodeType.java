package com.traffic.optimization.model;

/**
 * Node type enumeration
 */
public enum NodeType {
    /**
     * Internal intersection - controlled by a traffic light
     */
    INTERSECTION,

    /**
     * Boundary entry/exit point - origin and destination for traffic flows
     */
    BOUNDARY
}

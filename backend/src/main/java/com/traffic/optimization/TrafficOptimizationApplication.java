package com.traffic.optimization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Traffic signal optimization system - main application entry point
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@SpringBootApplication
public class TrafficOptimizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficOptimizationApplication.class, args);
        System.out.println("===========================================");
        System.out.println("Traffic Signal Optimization System started");
        System.out.println("Visit http://localhost:8080");
        System.out.println("===========================================");
    }
}

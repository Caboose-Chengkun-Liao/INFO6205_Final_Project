package com.traffic.optimization;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 交通信号优化系统 - 主应用入口
 *
 * @author Chengkun Liao, Mingjie Shen
 */
@SpringBootApplication
public class TrafficOptimizationApplication {

    public static void main(String[] args) {
        SpringApplication.run(TrafficOptimizationApplication.class, args);
        System.out.println("===========================================");
        System.out.println("交通信号优化系统已启动");
        System.out.println("访问 http://localhost:8080");
        System.out.println("===========================================");
    }
}

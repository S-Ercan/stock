package com.stock;

import com.stock.analyze.Analyzer;
import com.stock.collect.Collector;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan(basePackages = "com.stock.controller")
@EnableAutoConfiguration
public class StockApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class);
        new Collector().collect();
        new Analyzer().trainLRModel();
    }
}

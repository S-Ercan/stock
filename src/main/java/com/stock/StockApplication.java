package com.stock;

import com.stock.analyze.TrendCalculator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.util.Calendar;
import java.util.TimeZone;

@ComponentScan(basePackages = "com.stock.controller")
@EnableAutoConfiguration
public class StockApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class);
//        new Collector().collect();
//        new Analyzer().trainLRModel();

        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.add(Calendar.DATE, -60);
        new TrendCalculator().calculateTrend(today.getTime());
    }
}

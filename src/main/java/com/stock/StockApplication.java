package com.stock;

import com.stock.analyze.Analyzer;
import com.stock.analyze.TrendCalculator;
import com.stock.collect.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

import java.util.Calendar;
import java.util.TimeZone;

@ComponentScan(basePackages = "com.stock.controller")
@EnableAutoConfiguration
public class StockApplication {
    private static Logger log = LoggerFactory.getLogger(StockApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(StockApplication.class);

        if (args.length > 1) {
            log.error("Too many arguments.");
            System.exit(1);
        }

        if (args.length == 1) {
            String mode = args[0];
            switch (mode) {
                case "collect":
                    new Collector().collect();
                    System.exit(0);
                    break;
                case "analyze":
                    new Analyzer().createTrendModel();
                    break;
                case "trend":
                    Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    today.add(Calendar.DATE, -60);
                    new TrendCalculator().calculateTrends(today.getTime(), 5);
                    break;
                default:
                    log.error("Illegal argument.");
                    System.exit(1);
                    break;
            }
        }
    }
}

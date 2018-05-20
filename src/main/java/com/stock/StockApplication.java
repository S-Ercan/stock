package com.stock;

import com.stock.analyze.Analyzer;
import com.stock.collect.Collector;

public class StockApplication {
    public static void main(String[] args) {
//        new Collector().collect();
        new Analyzer().analyze();
    }
}

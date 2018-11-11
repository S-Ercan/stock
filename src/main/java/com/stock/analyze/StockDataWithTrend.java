package com.stock.analyze;

import java.io.Serializable;
import java.text.DecimalFormat;

public class StockDataWithTrend implements Serializable {
    private DecimalFormat priceFormat;

    private String symbol;

    private double difference;

    public StockDataWithTrend() {
        this.priceFormat = new DecimalFormat("0.00");
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public double getDifference() {
        return difference;
    }

    public void setDifference(double difference) {
        this.difference = difference;
    }

    @Override
    public String toString() {
        return String.format("Difference for symbol %s: %s.",
                this.getSymbol(), this.priceFormat.format(this.getDifference()));
    }
}

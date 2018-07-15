package com.stock.analyze;

import java.io.Serializable;

public class StockDataWithTrend implements Serializable {
    private String symbol;

    private int difference;

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public int getDifference() {
        return difference;
    }

    public void setDifference(int difference) {
        this.difference = difference;
    }
}

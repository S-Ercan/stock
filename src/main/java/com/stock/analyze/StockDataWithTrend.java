package com.stock.analyze;

import com.stock.entity.TimeSeries;

import java.io.Serializable;

public class StockDataWithTrend extends TimeSeries implements Serializable {
    public int getDifference() {
        return difference;
    }

    public void setDifference(int difference) {
        this.difference = difference;
    }

    private int difference;
}

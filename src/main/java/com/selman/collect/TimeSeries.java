package com.selman.collect;

import java.util.Map;

public class TimeSeries {
    public Map<String, Map<String, String>> getTimeSeries() {
        return timeSeries;
    }

    public void setTimeSeries(Map<String, Map<String, String>> timeSeries) {
        this.timeSeries = timeSeries;
    }

    private Map<String, Map<String, String>> timeSeries;
}

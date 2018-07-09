package com.stock.analyze;

import com.stock.dao.DAO;
import com.stock.entity.TimeSeries;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

public class TrendCalculator {
    private Logger log = LoggerFactory.getLogger(TrendCalculator.class);

    public void calculateTrend(Date startDate) {
        DecimalFormat priceFormat = new DecimalFormat("0.00");

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        String startDateString = dateFormat.format(startDate);

        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date endDate = today.getTime();
        String endDateString = dateFormat.format(endDate);

        log.info("Calculating difference between closing prices on {} and {}.", startDateString, endDateString);

        Query query = DAO.getSession().createQuery("" +
                "FROM TimeSeries " +
                "WHERE trading_day>='" + startDateString + "' " +
                "ORDER BY trading_day ASC"
        ).setMaxResults(1);
        List result = query.list();

        if (result.isEmpty()) {
            throw new IllegalStateException("Insufficient data for trend calculation.");
        }

        TimeSeries timeSeriesStart = (TimeSeries) result.get(0);
        log.info("Found entry on {} with closing price {}.", timeSeriesStart.getTradingDay().toString(), priceFormat.format(timeSeriesStart.getClosingPrice()));

        query = DAO.getSession().createQuery("" +
                "FROM TimeSeries " +
                "WHERE trading_day<='" + endDateString + "' " +
                "ORDER BY trading_day DESC"
        ).setMaxResults(1);
        result = query.list();

        if (result.isEmpty()) {
            throw new IllegalStateException("Insufficient data for trend calculation.");
        }

        TimeSeries timeSeriesEnd = (TimeSeries) result.get(0);
        log.info("Found entry on {} with closing price {}.", timeSeriesEnd.getTradingDay().toString(), priceFormat.format(timeSeriesEnd.getClosingPrice()));

        log.info("Difference: {}", priceFormat.format(timeSeriesEnd.getClosingPrice() - timeSeriesStart.getClosingPrice()));
    }
}

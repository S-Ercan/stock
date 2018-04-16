package com.stock.collect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stock.dao.DAO;
import com.stock.entity.TimeSeries;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Collector {
    private final Logger log = LoggerFactory.getLogger(Collector.class);
    private final String FUNCTION = "TIME_SERIES_DAILY";
    private final String SYMBOL = "MSFT";
    private final String OUTPUT_SIZE = "compact";
    private final String API_KEY = "W6F61C7U07E7E8JX";
    private String requestURLFormatString = "https://www.alphavantage.co/query?function=%s&symbol=%s&outputsize=%s&apikey=%s";

    public void collect() {
        Date firstDateToProcess = getLastProcessedDay();

        String requestURL = String.format(requestURLFormatString, FUNCTION, SYMBOL, OUTPUT_SIZE, API_KEY);
        String response = request(requestURL);
        JsonObject timeSeriesData = parseResponse(response);

        persistTimeSeries(timeSeriesData, firstDateToProcess);
        DAO.close();
    }

    private Date getLastProcessedDay() {
        Query query = DAO.getSession().createQuery("FROM TimeSeries ORDER BY trading_day DESC");
        List result = query.list();

        if (result.size() == 0) {
            Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
            today.add(Calendar.DATE, -100);
            return today.getTime();
        }

        TimeSeries timeSeries = (TimeSeries) result.get(0);
        return timeSeries.getTradingDay();
    }

    private String request(String targetURL) {
        String response = null;
        try {
            URL obj = new URL(targetURL);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod("GET");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder input = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                input.append(inputLine);
            }
            in.close();

            response = input.toString();
        } catch (ProtocolException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return response;
    }

    private JsonObject parseResponse(String response) {
        if (response == null) {
            log.warn("Empty response.");
            return null;
        }
        return new JsonParser().parse(response).getAsJsonObject();
    }

    private void persistTimeSeries(JsonObject timeSeriesData, Date lastProcessedDay) {
        log.info(lastProcessedDay.toString());
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (timeSeriesData == null) {
            log.warn("Empty JSON object for time series.");
            return;
        }

        Session session = DAO.getSession();
        log.info("Created session.");

        for (Map.Entry<String, JsonElement> entry : timeSeriesData.get("Meta Data").getAsJsonObject().entrySet()) {
            log.info(entry.getKey() + ": " + entry.getValue());
        }

        Transaction transaction = session.getTransaction();
        transaction.begin();

        String tradingDateString;
        Date tradingDate = null;
        for (Map.Entry<String, JsonElement> entry : timeSeriesData.get("Time Series (Daily)").getAsJsonObject().entrySet()) {
            tradingDateString = entry.getKey();
            try {
                tradingDate = df.parse(tradingDateString);
                if (!(tradingDate.after(lastProcessedDay))) {
                    continue;
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }

            JsonObject values = entry.getValue().getAsJsonObject();

            TimeSeries timeSeries = new TimeSeries();
            timeSeries.setTradingDay(tradingDate);
            timeSeries.setOpeningPrice(values.get("1. open").getAsDouble());
            timeSeries.setHigh(values.get("2. high").getAsDouble());
            timeSeries.setLow(values.get("3. low").getAsDouble());
            timeSeries.setClosingPrice(values.get("4. close").getAsDouble());
            timeSeries.setVolume(values.get("5. volume").getAsLong());
            session.persist(timeSeries);
        }

        transaction.commit();
        log.info("Persisted data.");
    }
}

package com.stock.collect;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stock.dao.DAO;
import com.stock.entity.Symbol;
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
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class Collector {
    private final Logger log = LoggerFactory.getLogger(Collector.class);
    private final String FUNCTION = "TIME_SERIES_DAILY";
    private final String OUTPUT_SIZE = "compact";
    private final String API_KEY = "W6F61C7U07E7E8JX";
    private final int maxRequestsPerMinute = 4;
    private final long requestDelay = 60000;
    private final String requestURLFormatString = "https://www.alphavantage.co/query?function=%s&symbol=%s&outputsize=%s&apikey=%s";

    public void collect() {
        List<String> symbols = this.getSymbolsToCollect();

        int numRequests = 0;
        for (String symbol : symbols) {
            if (numRequests > 0 && numRequests % this.maxRequestsPerMinute == 0) {
                try {
                    Thread.sleep(this.requestDelay);
                } catch (InterruptedException e) {
                    log.warn("Thread.sleep() was interrupted.");
                }
            }
            this.collectForSymbol(symbol);
            numRequests++;
        }

        DAO.close();
    }

    public void collectForSymbol(String symbol) {
        log.info("Collecting data for symbol {}.", symbol);
        Date firstDateToProcess = getLastProcessedDay(symbol);

        String requestURL = String.format(requestURLFormatString, FUNCTION, symbol, OUTPUT_SIZE, API_KEY);
        String response = request(requestURL);
        JsonObject timeSeriesData = parseResponse(response);

        persistTimeSeries(timeSeriesData, firstDateToProcess, symbol);
    }

    private List<String> getSymbolsToCollect() {
        List<String> symbols = new ArrayList<>();

        Query query = DAO.getSession().createQuery("FROM Symbol WHERE collect=1");
        List<Symbol> result = query.list();

        for (Symbol symbol : result) {
            symbols.add(symbol.getSymbol());
        }

        return symbols;
    }

    private Date getLastProcessedDay(String symbol) {
        Query query = DAO.getSession().createQuery("FROM TimeSeries WHERE symbol = :symbol ORDER BY trading_day DESC");
        query.setParameter("symbol", symbol);
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

    private void persistTimeSeries(JsonObject timeSeriesData, Date lastProcessedDay, String symbol) {
        log.info("Persisting data for symbol {}.", symbol);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));

        if (timeSeriesData == null) {
            log.warn("Empty JSON object for {}.", symbol);
            return;
        }
        JsonElement metadataElement = timeSeriesData.get("Meta Data");
        if (metadataElement == null) {
            log.warn("No 'Meta Data' element found in response for {}.", symbol);
            return;
        }

        Session session = DAO.getSession();
        log.info("Created session.");

        for (Map.Entry<String, JsonElement> entry : metadataElement.getAsJsonObject().entrySet()) {
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
            timeSeries.setSymbol(symbol);
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

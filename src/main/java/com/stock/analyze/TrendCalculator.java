package com.stock.analyze;

import com.stock.dao.DAO;
import com.stock.entity.Symbol;
import com.stock.entity.TimeSeries;
import org.apache.spark.SparkConf;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.clustering.KMeans;
import org.apache.spark.ml.clustering.KMeansModel;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.sql.*;
import org.hibernate.query.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ThreadLocalRandom;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class TrendCalculator {
    private Logger log = LoggerFactory.getLogger(TrendCalculator.class);

    private DecimalFormat priceFormat;
    private SimpleDateFormat dateFormat;

    public TrendCalculator() {
        this.priceFormat = new DecimalFormat("0.00");
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd");
    }

    public List<HashMap<String, List<HashMap<String, Object>>>> calculate() {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.add(Calendar.DATE, -60);
        return this.calculateTrends(today.getTime(), 5);
    }

    public List<HashMap<String, List<HashMap<String, Object>>>> calculateFromStartDate(int daysAgo) {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.add(Calendar.DATE, -daysAgo);
        return this.calculateTrends(today.getTime(), 5);
    }

    public List<HashMap<String, List<HashMap<String, Object>>>> calculateWithNumberOfClusters(int numberOfClusters) {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.add(Calendar.DATE, -60);
        return this.calculateTrends(today.getTime(), numberOfClusters);
    }

    public List<HashMap<String, List<HashMap<String, Object>>>> calculateFromStartDateAndWithNumberOfClusters(int daysAgo, int numberOfClusters) {
        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        today.add(Calendar.DATE, daysAgo);
        return this.calculateTrends(today.getTime(), numberOfClusters);
    }

    public List<HashMap<String, List<HashMap<String, Object>>>> calculateTrends(Date startDate, int numberOfClusters) {
        log.info("Calculating {} clusters starting from {}.", numberOfClusters, startDate);
        List<String> symbols = this.getSymbolsToAnalyze();
        List<StockDataWithTrend> stockDataWithTrends = new ArrayList<>();
        for (String symbol : symbols) {
            try {
                stockDataWithTrends.add(this.calculateTrendForSymbol(symbol, startDate));
            } catch (IllegalStateException e) {
                log.warn("Could not calculate trend for symbol {}, skipping.", symbol);
            }
        }
        return this.cluster(stockDataWithTrends, numberOfClusters);
    }

    private List<String> getSymbolsToAnalyze() {
        List<String> symbols = new ArrayList<>();

        Query query = DAO.getSession().createQuery("FROM Symbol WHERE collect=1");
        List<Symbol> result = query.list();

        for (Symbol symbol : result) {
            symbols.add(symbol.getSymbol());
        }

        return symbols;
    }

    public StockDataWithTrend calculateTrendForSymbol(String symbol, Date startDate) {
        String startDateString = this.dateFormat.format(startDate);

        Calendar today = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        Date endDate = today.getTime();
        String endDateString = this.dateFormat.format(endDate);

        log.info("Calculating difference between closing prices on {} and {}.", startDateString, endDateString);

        Query query = DAO.getSession().createQuery("" +
                "FROM TimeSeries " +
                "WHERE symbol = :symbol AND trading_day >= :startDate " +
                "ORDER BY trading_day ASC"
        ).setParameter("symbol", symbol).setParameter("startDate", startDateString).setMaxResults(1);
        List result = query.list();

        if (result.isEmpty()) {
            throw new IllegalStateException("Insufficient data for trend calculation.");
        }

        TimeSeries timeSeriesStart = (TimeSeries) result.get(0);
        log.info("Found entry on {} with closing price {}.", timeSeriesStart.getTradingDay().toString(), this.priceFormat.format(timeSeriesStart.getClosingPrice()));

        query = DAO.getSession().createQuery("" +
                "FROM TimeSeries " +
                "WHERE symbol = :symbol AND trading_day <= :endDate " +
                "ORDER BY trading_day DESC"
        ).setParameter("symbol", symbol).setParameter("endDate", endDateString).setMaxResults(1);
        result = query.list();

        if (result.isEmpty()) {
            throw new IllegalStateException("Insufficient data for trend calculation.");
        }

        TimeSeries timeSeriesEnd = (TimeSeries) result.get(0);
        log.info("Found entry on {} with closing price {}.", timeSeriesEnd.getTradingDay().toString(), this.priceFormat.format(timeSeriesEnd.getClosingPrice()));

        double difference = timeSeriesEnd.getClosingPrice() - timeSeriesStart.getClosingPrice();
        StockDataWithTrend stockDataWithTrend = new StockDataWithTrend();
        stockDataWithTrend.setSymbol(symbol);
        stockDataWithTrend.setDifference(difference);

        log.info(stockDataWithTrend.toString());

        return stockDataWithTrend;
    }

    private List<HashMap<String, List<HashMap<String, Object>>>> cluster(List<StockDataWithTrend> stockDataWithTrends, int numberOfClusters) {
        Encoder<StockDataWithTrend> encoder = Encoders.bean(StockDataWithTrend.class);

        SparkConf sparkConf = this.getSparkConf();
        SparkSession spark = this.getSparkSession(sparkConf);
        Dataset<StockDataWithTrend> dataset = spark.createDataset(stockDataWithTrends, encoder);

        dataset.show();

        String[] inputColumns = new String[]{"difference"};
        VectorAssembler vectorAssembler = new VectorAssembler().setInputCols(inputColumns).setOutputCol("features");

        KMeans kmeans = new KMeans().setK(numberOfClusters).setMaxIter(10).setSeed(1L);

        Pipeline pipeline = new Pipeline();
        pipeline.setStages(new PipelineStage[]{vectorAssembler, kmeans});
        PipelineModel pipelineModel = pipeline.fit(dataset);
        KMeansModel kMeansModel = (KMeansModel) pipelineModel.stages()[1];

        log.info(Arrays.toString(kMeansModel.clusterCenters()));
        kMeansModel.summary().cluster().show();

        Dataset<Row> predictions = kMeansModel.transform(vectorAssembler.transform(dataset));
        predictions.show();

        DAO.close();

        return this.formatOutput(predictions, numberOfClusters);
    }

    private List<HashMap<String, List<HashMap<String, Object>>>> formatOutput(Dataset<Row> predictions, int k) {
        List<HashMap<String, List<HashMap<String, Object>>>> output = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            HashMap<String, List<HashMap<String, Object>>> data = new HashMap<>();
            data.put("data", new ArrayList<>());
            output.add(data);
        }

        int randomMargin;
        for (Row prediction : predictions.collectAsList()) {
            String symbol = prediction.get(prediction.fieldIndex("symbol")).toString();
            double difference = (double) prediction.get(prediction.fieldIndex("difference"));
            int predictedCluster = (int) prediction.get(prediction.fieldIndex("prediction"));

            HashMap<String, Object> point = new HashMap<>();
            randomMargin = ThreadLocalRandom.current().nextInt(-2, 3);
            point.put("x", predictedCluster * 5 + 20 + randomMargin);
            point.put("y", difference);
            point.put("z", 1);
            point.put("name", symbol);
            output.get(predictedCluster).get("data").add(point);
        }
        return output;
    }

    private SparkConf getSparkConf() {
        return new SparkConf().setAppName("stock").setMaster("local").set("spark.testing.memory", "2147480000");
    }

    private SparkSession getSparkSession(SparkConf sparkConf) {
        return SparkSession
                .builder()
                .appName("Stock")
                .config(sparkConf)
                .getOrCreate();
    }
}

package com.stock.analyze;

import org.apache.spark.SparkConf;
import org.apache.spark.ml.Pipeline;
import org.apache.spark.ml.PipelineModel;
import org.apache.spark.ml.PipelineStage;
import org.apache.spark.ml.feature.VectorAssembler;
import org.apache.spark.ml.regression.LinearRegression;
import org.apache.spark.ml.regression.LinearRegressionModel;
import org.apache.spark.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

import static org.apache.spark.sql.functions.avg;
import static org.apache.spark.sql.functions.stddev;

public class Analyzer {
    private Logger log = LoggerFactory.getLogger(Analyzer.class);

    public void detectAnomalies(Dataset<Row> dataset) {
        Row meanRow = dataset.select(avg("low")).first();
        double mean = meanRow.getDouble(0);
        Row stddevRow = dataset.select(stddev("low")).first();
        double stddev = stddevRow.getDouble(0);
        log.info("mean: {}, stddev: {}", mean, stddev);

        double low;
        int lowIndex = dataset.first().fieldIndex("low");
        for (Row row : dataset.collectAsList()) {
            low = row.getDouble(lowIndex);
            if ((low < (mean - 2 * stddev)) || (low > (mean + 2 * stddev))) {
                log.info("low: {}", row.toString());
            }
        }
    }

    public void trainLRModel() {
        SparkConf conf = this.getSparkConf();
        SparkSession spark = this.getSparkSession(conf);
        Dataset<Row> dataset = this.readAsDataset(spark, "time_series");

        String[] inputColumns = new String[]{"opening_price", "volume", "high", "low"};
        VectorAssembler vectorAssembler = new VectorAssembler().setInputCols(inputColumns).setOutputCol("features");

        LinearRegression linearRegression = new LinearRegression().setMaxIter(10).setFeaturesCol("features").setLabelCol("closing_price");

        Pipeline pipeline = new Pipeline();
        pipeline.setStages(new PipelineStage[]{vectorAssembler, linearRegression});
        PipelineModel pipelineModel = pipeline.fit(dataset);
        LinearRegressionModel linearRegressionModel = (LinearRegressionModel) pipelineModel.stages()[1];

        log.info("Coefficients: {}", linearRegressionModel.coefficients().toString());
        log.info("Intercept: {}", linearRegressionModel.intercept());
    }

    public void createTrendModel() {
        StockDataWithTrend data = new StockDataWithTrend();
        Encoder<StockDataWithTrend> encoder = Encoders.bean(StockDataWithTrend.class);

        SparkConf sparkConf = this.getSparkConf();
        SparkSession spark = this.getSparkSession(sparkConf);
        Dataset<StockDataWithTrend> dataset = spark.createDataset(Collections.singletonList(data), encoder);
        dataset.show();
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

    private Dataset<Row> readAsDataset(SparkSession spark, String tableName) {
        return spark.read()
                .format("jdbc")
                .option("url", "jdbc:mysql://localhost/stock?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC")
                .option("dbtable", tableName)
                .option("user", "stock")
                .option("password", "stock")
                .load();
    }
}

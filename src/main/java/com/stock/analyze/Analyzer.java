package com.stock.analyze;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.spark.sql.functions.*;

public class Analyzer {
    private Logger log = LoggerFactory.getLogger(Analyzer.class);

    public void analyze() {
        SparkConf conf = new SparkConf().setAppName("stock").setMaster("local").set("spark.testing.memory", "2147480000");

        SparkSession spark = SparkSession
                .builder()
                .appName("Stock")
                .config(conf)
                .getOrCreate();

        Dataset<Row> dataset = spark.read()
                .format("jdbc")
                .option("url", "jdbc:mysql://localhost/stock?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC")
                .option("dbtable", "time_series")
                .option("user", "stock")
                .option("password", "stock")
                .load();

        this.detectAnomalies(dataset);
    }

    private void detectAnomalies(Dataset<Row> dataset) {
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
}

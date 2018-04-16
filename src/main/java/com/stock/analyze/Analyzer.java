package com.stock.analyze;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.spark.sql.functions.*;

public class Analyzer {
    private final Logger log = LoggerFactory.getLogger(Analyzer.class);

    public void analyze() {
        SparkConf conf = new SparkConf().setAppName("stock").setMaster("local");

        SparkSession spark = SparkSession
                .builder()
                .appName("Stock")
                .config(conf)
                .getOrCreate();

        Dataset<Row> jdbcDF = spark.read()
                .format("jdbc")
                .option("url", "jdbc:mysql://localhost/stock?useJDBCCompliantTimezoneShift=true&useLegacyDatetimeCode=false&serverTimezone=UTC")
                .option("dbtable", "time_series")
                .option("user", "stock")
                .option("password", "stock")
                .load();

//        for (Row row : jdbcDF.collectAsList()) {
//            log.info(String.join(",", jdbcDF.columns()));
//            log.info(row.toString());
//        }
        jdbcDF.select(min("low")).show();
        jdbcDF.select(avg("low")).show();
        jdbcDF.select(max("low")).show();
    }
}

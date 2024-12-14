package com.ksoot.spark.sales;

import static org.apache.spark.sql.functions.col;
import static org.apache.spark.sql.functions.sum;

import com.ksoot.spark.common.connector.ArangoConnector;
import com.ksoot.spark.common.connector.FileConnector;
import com.ksoot.spark.common.connector.MongoConnector;
import com.ksoot.spark.common.util.SparkUtils;
import com.ksoot.spark.sales.conf.JobProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.types.DataTypes;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class SparkPipelineExecutor {

  private final SparkSession sparkSession;

  private final JobProperties jobProperties;

  private final FileConnector fileConnector;

  private final MongoConnector mongoConnector;

  private final ArangoConnector arangoConnector;

  public void execute() {
    log.info("Generating Daily sales report for month: {}", this.jobProperties.getMonth());

    Dataset<Row> salesDataset = this.mongoConnector.read("sales");
    SparkUtils.logDataset("Sales Dataset", salesDataset);

    final String statementMonth = this.jobProperties.getMonth().toString();
    // Convert `timestamp` to date and calculate daily sales amount
    Dataset<Row> aggregatedSales =
        salesDataset
            .filter(col("timestamp").startsWith(statementMonth))
            .withColumn("date", col("timestamp").substr(0, 10)) // Extract date part
            .withColumn(
                "sale_amount",
                col("price")
                    .cast(DataTypes.DoubleType)
                    .multiply(col("quantity").cast(DataTypes.IntegerType))) // Calculate sale_amount
            .groupBy("product_id", "date")
            .agg(sum("sale_amount").alias("daily_sale_amount"));

    Dataset<Row> productsDataset = this.arangoConnector.readAll("products");
    SparkUtils.logDataset("Products Dataset", salesDataset);
    productsDataset =
        productsDataset.select(col("_key").as("product_id"), col("name").as("product_name"));

    // Join with the product details dataset
    Dataset<Row> dailySalesReport =
        aggregatedSales
            .join(
                productsDataset,
                aggregatedSales.col("product_id").equalTo(productsDataset.col("product_id")))
            .select(
                productsDataset.col("product_name").as("product"),
                aggregatedSales.col("date"),
                aggregatedSales.col("daily_sale_amount").alias("sale"))
            .orderBy(col("product_name"), col("date"));

    // Show the final result
    SparkUtils.logDataset("Daily Sales report", dailySalesReport, 1000);

    final String salesReportCollection = "sales_report_" + statementMonth.replace('-', '_');
    //    this.fileConnector.write(dailySalesReport); // For testing
    this.mongoConnector.write(dailySalesReport, salesReportCollection);
  }
}

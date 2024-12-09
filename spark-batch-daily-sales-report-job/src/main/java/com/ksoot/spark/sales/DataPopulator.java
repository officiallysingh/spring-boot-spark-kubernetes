package com.ksoot.spark.sales;

import static com.ksoot.spark.common.util.DateTimeUtils.ZONE_ID_IST;

import com.arangodb.springframework.core.ArangoOperations;
import com.mongodb.client.MongoCollection;
import java.time.*;
import java.util.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.datafaker.Faker;
import org.apache.commons.collections4.CollectionUtils;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DataPopulator {

  private static final Faker faker =
      new Faker(new Locale.Builder().setLanguage("en").setRegion("US").build());
  private static final String[] products = {
    "1001", "1002", "1003", "1004", "1005", "1006", "1007", "1008", "1009", "1010"
  };

  // Total number of Credit card accounts to be created
  // For each account upto 10 transactions are created for each day of last 3 months
  private static final int SALES_COUNT = 1000;

  // Number of records to be created in a batch
  private static final int BATCH_SIZE = 1000;

  private static final String SALES_COLLECTION = "sales";
  private final MongoOperations mongoOperations;

  private static final String PRODUCTS_COLLECTION = "products";
  private final ArangoOperations arangoOperations;

  public void populateData() {
    this.createProductsData();
    this.createSalesSchema();
    this.createSalesData();
  }

  private void createProductsData() {
    log.info("Creating Products data");
    if (this.arangoOperations.collection(PRODUCTS_COLLECTION).count() > 0) {
      log.info("Data already exists: {}", PRODUCTS_COLLECTION);
    } else {
      this.arangoOperations.insert(Product.of("1001", "TV"));
      this.arangoOperations.insert(Product.of("1002", "Mobile"));
      this.arangoOperations.insert(Product.of("1003", "Table"));
      this.arangoOperations.insert(Product.of("1004", "Chair"));
      this.arangoOperations.insert(Product.of("1005", "Sofa"));
      this.arangoOperations.insert(Product.of("1006", "AC"));
      this.arangoOperations.insert(Product.of("1007", "Bed"));
      this.arangoOperations.insert(Product.of("1008", "Charger"));
      this.arangoOperations.insert(Product.of("1009", "Laptop"));
      this.arangoOperations.insert(Product.of("1010", "Tablet"));
      log.info("Created Products data");
    }
    this.arangoOperations.driver().shutdown();
  }

  private void createSalesSchema() {
    log.info("Creating Sales collection Schema");
    if (!this.mongoOperations.collectionExists(SALES_COLLECTION)) {
      this.mongoOperations.createCollection(SALES_COLLECTION);
      log.info("Created Collection: {}", SALES_COLLECTION);
      final Index indexTimestamp =
          new Index().named("idx_timestamp").on("timestamp", Sort.Direction.ASC);
      this.mongoOperations.indexOps(SALES_COLLECTION).ensureIndex(indexTimestamp);
    } else {
      log.info("Collection already exists: {}", SALES_COLLECTION);
    }
  }

  private void createSalesData() {
    log.info("Creating Sales data");
    final MongoCollection<Document> salesCollection =
        this.mongoOperations.getCollection(SALES_COLLECTION);
    if (salesCollection.countDocuments() > 0) {
      log.info("Sales data already exists");
      return;
    }

    final YearMonth currentMonth = YearMonth.now(ZONE_ID_IST);
    final List<YearMonth> months =
        List.of(
            currentMonth.minusMonths(3), currentMonth.minusMonths(2), currentMonth.minusMonths(1));
    int recordCount = 0;
    final List<Document> sales = new ArrayList<>(BATCH_SIZE);
    for (int i = 1; i <= SALES_COUNT; i++) {
      for (final YearMonth month : months) {
        for (LocalDate date = month.atDay(1);
            !date.isAfter(month.atEndOfMonth());
            date = date.plusDays(1)) {

          int salesPerDay = faker.number().numberBetween(1, 11);
          for (int j = 0; j < salesPerDay; j++) {

            final String transactionId = faker.internet().uuid();
            final long time = faker.time().between(LocalTime.MIN, LocalTime.MAX);
            final LocalDateTime timestamp = LocalDateTime.of(date, LocalTime.ofNanoOfDay(time));
            final String productId = new Faker().options().option(products);
            final int quantity = faker.number().numberBetween(1, 6);
            final double price = faker.number().randomDouble(2, 10, 1000);

            final Document sale = new Document("_id", new ObjectId());
            sale.append("transaction_id", transactionId)
                .append("timestamp", timestamp)
                .append("product_id", productId)
                .append("quantity", quantity)
                .append("price", price);
            sales.add(sale);

            recordCount++;
            if (recordCount % BATCH_SIZE == 0) {
              salesCollection.insertMany(sales);
              sales.clear();
              log.info("Created {} Sales transactions, processed for date: {}", recordCount, date);
            }
          }
        }
      }
    }

    if (CollectionUtils.isNotEmpty(sales)) {
      salesCollection.insertMany(sales);
      sales.clear();
    }
    log.info("Created {} Sales transactions", recordCount);
  }

  @Getter
  @AllArgsConstructor(staticName = "of")
  @com.arangodb.springframework.annotation.Document("products")
  static class Product {

    @Id private String id;

    private String name;
  }
}

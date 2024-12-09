package com.ksoot.spark.common.connector;

import static org.apache.commons.lang3.StringUtils.*;

import com.ksoot.spark.common.config.properties.ConnectorProperties;
import com.ksoot.spark.common.util.JobConstants;
import com.ksoot.spark.common.util.SparkOptions;
import com.ksoot.spark.common.util.StringUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.springframework.util.Assert;

@Slf4j
@RequiredArgsConstructor
public class ArangoConnector {
  public static final String VAR_COLLECTION = "collection";
  private static final String VAR_FILTER = "filter";
  private static final String VAR_PROJECTION = "projection";

  private static final String SELECT_TEMPLATE =
      "FOR ${" + VAR_COLLECTION + "} IN ${" + VAR_COLLECTION + "}";
  private static final String FILTER_TEMPLATE = "FILTER ${" + VAR_FILTER + "}";
  private static final String RETURN_PROJECTION_TEMPLATE = "RETURN {${" + VAR_PROJECTION + "}}";
  private static final String RETURN_COLLECTION_TEMPLATE = "RETURN ${" + VAR_COLLECTION + "}";

  protected final SparkSession sparkSession;

  protected final ConnectorProperties properties;

  public Dataset<Row> readAll(final String collection) {
    log.info("Fetching Knowledge >> collection: {}", collection);
    Assert.hasText(collection, "ArangoDB collection name required");
    return this.sparkSession
        .read()
        .options(this.properties.getArangoOptions().options())
        .option(SparkOptions.Arango.TABLE, collection)
        .option(SparkOptions.Common.INFER_SCHEMA, true)
        .load();
  }

  public Dataset<Row> read(final String query) {
    log.info("Fetching Knowledge >> query: {}", query);
    Assert.hasText(query, "ArangoDB query required");
    return this.sparkSession
        .read()
        .options(this.properties.getArangoOptions().options())
        .option(SparkOptions.Arango.QUERY, query)
        .option(SparkOptions.Common.INFER_SCHEMA, true)
        .load();
  }

  public Dataset<Row> read(final String collection, final String filter, final String projection) {
    Assert.hasText(collection, "ArangoDB collection name required");
    log.info(
        "Fetching Knowledge >> collection: {}, filter: {}, projection: {}",
        collection,
        filter,
        projection);
    if (isBlank(filter) && isBlank(projection)) {
      return this.readAll(collection);
    } else {
      final String selectClause =
          StringUtils.substitute(SELECT_TEMPLATE, VAR_COLLECTION, collection);
      final String filterClause =
          isNotBlank(filter)
              ? StringUtils.substitute(FILTER_TEMPLATE, VAR_FILTER, filter)
              : JobConstants.BLANK;

      final String returnClause;
      if (isNotBlank(projection)) {
        final String finalProjection =
            StringUtils.substitute(projection, VAR_COLLECTION, collection);
        returnClause =
            StringUtils.substitute(RETURN_PROJECTION_TEMPLATE, VAR_PROJECTION, finalProjection);
      } else {
        returnClause =
            StringUtils.substitute(RETURN_COLLECTION_TEMPLATE, VAR_COLLECTION, collection);
      }

      final String query = this.createQuery(selectClause, filterClause, returnClause);
      return this.read(query);
    }
  }

  private String createQuery(
      final String selectClause, final String filterClause, final String returnClause) {
    return (selectClause.trim()
            + StringUtils.prependIfNotBlank(filterClause, SPACE)
            + StringUtils.prependIfNotBlank(returnClause, SPACE))
        .trim();
  }

  // TODO: Implement any other reader methods with Schema arguments and writer methods
}

package com.ksoot.spark.common.util;

import static com.ksoot.spark.common.util.JobConstants.BACKTICK;
import static com.ksoot.spark.common.util.JobConstants.DOT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.functions;
import org.apache.spark.sql.types.ArrayType;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

public class SparkJsonUtils {

  public static Dataset<Row> flattenJsonData(Dataset<Row> rawData) {
    List<String> flattenedColumns = new ArrayList<>();

    for (StructField structField : rawData.schema().fields()) {
      String fieldName = structField.name();

      if (structField.dataType() instanceof StructType) {
        flattenNestedJson(structField, flattenedColumns, fieldName);
      } else if (structField.dataType() instanceof ArrayType) {
        rawData = rawData.withColumn(fieldName, functions.explode(rawData.col(fieldName)));
        flattenJsonArray(structField, flattenedColumns, fieldName);
      } else {
        flattenedColumns.add(fieldName);
      }
    }
    rawData = rawData.selectExpr(flattenedColumns.toArray(new String[0]));
    return rawData;
  }

  private static Collection<String> flattenNestedJson(
      StructField structField, List<String> flatColumns, String fieldName) {
    if (structField.dataType() instanceof StructType nestedStruct) {

      for (StructField nestedField : nestedStruct.fields()) {
        String nestedFieldName = fieldName + "." + nestedField.name();
        if (nestedField.dataType() instanceof StructType) {
          flattenNestedJson(nestedField, flatColumns, nestedFieldName);
        } else if (nestedField.dataType() instanceof ArrayType) {
          flattenJsonArray(nestedField, flatColumns, nestedFieldName);
        } else {
          flatColumns.add(nestedFieldName + " AS " + StringUtils.wrap(nestedFieldName, BACKTICK));
        }
      }
    } else {
      flatColumns.add(structField.name());
    }
    return flatColumns;
  }

  private static List<String> flattenJsonArray(
      StructField structField, List<String> flatColumns, String fieldName) {
    ArrayType arrayType = (ArrayType) structField.dataType();
    if (arrayType.elementType() instanceof StructType nestedStruct) {
      for (StructField nestedField : nestedStruct.fields()) {
        String nestedFieldName = fieldName + DOT + nestedField.name();
        flatColumns.add(nestedFieldName + " AS " + StringUtils.wrap(nestedFieldName, BACKTICK));
      }
    } else {
      flatColumns.add(fieldName);
    }
    return flatColumns;
  }
}

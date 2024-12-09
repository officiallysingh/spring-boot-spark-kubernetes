package com.ksoot.spark.common.executor.publish;

import static com.ksoot.spark.common.util.JobConstants.*;
import static com.ksoot.spark.common.util.SparkUtils.logDataset;

import com.ksoot.spark.common.executor.Executor;
import com.ksoot.spark.common.util.JobConstants;
import com.ksoot.spark.common.util.SparkOptions;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.spark.sql.*;

@Slf4j
@RequiredArgsConstructor
public class PublishExecutor implements Executor<Dataset<Row>, JobOutput> {

  private final PublishProperties publishProperties;

  public JobOutput execute(final Dataset<Row> dataset) {
    Dataset<Row> result = dataset;

    final String currentDateTime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
    final String outputLocation =
        this.publishProperties.getPath()
            + (this.publishProperties.getPath().endsWith(SLASH) ? BLANK : SLASH)
            + currentDateTime;

    result =
        this.publishProperties.isMerge()
            ? result.coalesce(1) // Write to a single file
            : result;
    logDataset("Spark Job Output", result);

    this.writeFile(result, this.publishProperties.getFormat(), outputLocation);
    log.info(
        "Job output files generated at location: {} in {} format. Please look for files with name part-<some generated string>.{}",
        outputLocation,
        this.publishProperties.getFormat(),
        this.publishProperties.getFormat());

    return this.buildJobOutput(outputLocation);
  }

  private JobOutput buildJobOutput(final String outputLocation) {
    final Path dir = Paths.get(outputLocation);
    try (Stream<Path> paths = Files.list(dir)) {
      final List<String> files =
          paths
              .filter(Files::isRegularFile)
              .map(Path::toAbsolutePath)
              .map(Path::toString)
              .filter(
                  fileName ->
                      fileName.endsWith(JobConstants.DOT + this.publishProperties.getFormat()))
              .toList();
      return JobOutput.of(
          dir.toAbsolutePath().toString(), files, this.publishProperties.getFormat());
    } catch (final IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  private void writeFile(final Dataset<Row> resultDf, final String format, final String path) {
    final DataFrameWriter<Row> dataFrameWriter =
        resultDf
            .write()
            .mode(this.publishProperties.getSaveMode())
            .option(SparkOptions.Common.HEADER, true) // Include header
            .option("nullValue", "null");
    if (format.equals(SparkOptions.CSV.FORMAT)) {
      dataFrameWriter.csv(path);
    } else if (format.equals(SparkOptions.Parquet.FORMAT)) {
      dataFrameWriter.parquet(path);
    } else {
      throw new IllegalArgumentException("Invalid file write format: " + format);
    }
  }
}

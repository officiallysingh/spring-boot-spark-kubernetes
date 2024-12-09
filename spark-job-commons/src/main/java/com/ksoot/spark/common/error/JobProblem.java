package com.ksoot.spark.common.error;

import static com.ksoot.spark.common.error.JobErrorType.BAD_REQUEST_TITLE;

import com.ksoot.spark.common.util.StringUtils;
import jakarta.annotation.Nullable;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Objects;
import lombok.Getter;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.builder.Builder;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.core.NestedRuntimeException;
import org.springframework.util.Assert;

@Getter
public class JobProblem extends NestedRuntimeException {

  private static final String REASON_TEMPLATE =
      "code: ${code}, title: ${title}, message: ${message}, cause: ${cause}, args: ${args}";

  private final ErrorType errorType;

  private final Object[] args;

  private JobProblem(final ErrorType errorType, final Throwable cause, final Object[] args) {
    super(reason(errorType.code(), errorType.title(), errorType.message(), cause, args), cause);
    this.errorType = errorType;
    this.args = args;
  }

  private static String reason(
      final String code,
      final String title,
      final String message,
      final Throwable cause,
      final Object[] args) {
    return StringUtils.substitute(
        REASON_TEMPLATE,
        "code",
        code,
        "title",
        title,
        "message",
        message,
        "cause",
        (Objects.nonNull(cause) ? cause.getMessage() : ""),
        "args",
        (ArrayUtils.isNotEmpty(args) ? ArrayUtils.toString(args) : ""));
  }

  public String getStackTrace(final Throwable exception) {
    String stacktrace = ExceptionUtils.getStackTrace(exception);
    StringBuilder escapedStacktrace = new StringBuilder(stacktrace.length() + 100);
    StringCharacterIterator scitr = new StringCharacterIterator(stacktrace);

    char current = scitr.first();
    // DONE = \\uffff (not a character)
    String lastAppend = null;
    while (current != CharacterIterator.DONE) {
      if (current == '\t' || current == '\r' || current == '\n') {
        if (!" ".equals(lastAppend)) {
          escapedStacktrace.append(" ");
          lastAppend = " ";
        }
      } else {
        // nothing matched - just text as it is.
        escapedStacktrace.append(current);
        lastAppend = "" + current;
      }
      current = scitr.next();
    }
    return escapedStacktrace.toString();
  }

  @Override
  public String toString() {
    return this.getMessage();
  }

  // ------------- Builder ---------------
  public static CauseBuilder of(final String message) {
    return new SparkProblemBuilder(JobErrorType.of(message));
  }

  public static CauseBuilder of(final String title, final String message) {
    return new SparkProblemBuilder(JobErrorType.of(title, message));
  }

  public static CauseBuilder of(final ErrorType errorType) {
    return new SparkProblemBuilder(errorType);
  }

  public static JobProblem badRequest(final String message) {
    return new JobProblem(JobErrorType.of(BAD_REQUEST_TITLE, message), null, null);
  }

  public interface CauseBuilder extends ArgsBuilder {
    ArgsBuilder cause(@Nullable Throwable cause);
  }

  public interface ArgsBuilder extends Builder<JobProblem> {
    Builder<JobProblem> args(@Nullable final Object... args);
  }

  static class SparkProblemBuilder implements CauseBuilder {

    private ErrorType errorType;
    private Throwable cause;
    private Object[] args;

    SparkProblemBuilder(final ErrorType errorType) {
      Assert.notNull(errorType, "'errorType' must not be null");
      this.errorType = errorType;
    }

    @Override
    public ArgsBuilder cause(@Nullable final Throwable cause) {
      this.cause = cause;
      return this;
    }

    @Override
    public Builder<JobProblem> args(@Nullable final Object[] args) {
      this.args = args;
      return this;
    }

    @Override
    public JobProblem build() {
      return new JobProblem(this.errorType, this.cause, this.args);
    }
  }
}

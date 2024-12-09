package com.ksoot.spark.common.util;

import static com.ksoot.spark.common.util.JobConstants.*;
import static org.apache.commons.lang3.StringUtils.*;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.util.Assert;

@UtilityClass
public class StringUtils {

  private static final String BRACKET_WRAP_TEMPLATE = "(%s)";

  public static String substitute(final String template, final String... vars) {
    Assert.hasText(template, "template is required");
    Assert.notEmpty(vars, "vars is required");
    if (vars.length % 2 != 0) {
      throw new IllegalArgumentException(
          "vars array must have an even number of elements, to create a Map.");
    }
    final Map<String, String> params =
        IntStream.range(0, vars.length / 2)
            .mapToObj(i -> ImmutablePair.of(vars[2 * i], vars[2 * i + 1]))
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    return substitute(template, params);
  }

  public static String substitute(final String template, final Map<String, String> vars) {
    Assert.hasText(template, "template is required");
    Assert.notEmpty(vars, "vars is required");
    return StringSubstitutor.replace(template, vars);
  }

  public static String bracketWrap(final String value) {
    Assert.hasText(value, "value is required");
    return String.format(BRACKET_WRAP_TEMPLATE, value.trim());
  }

  public static String backtickWrapIfNumeric(final String value) {
    Assert.hasText(value, "value is required");
    return isNumeric(value) ? wrap(value, BACKTICK) : value;
  }

  public static String backtickWrapIfContainsSlash(final String value) {
    Assert.hasText(value, "value is required");
    return contains(value, SLASH) ? wrap(value, BACKTICK) : value;
  }

  public static String prependIfNotBlank(final String value, String prefix) {
    Assert.notNull(value, "value is required");
    Assert.notNull(prefix, "prefix is required");
    return isNotBlank(value) ? prefix + value.trim() : BLANK;
  }

  public static String chompBeforeLastSlash(final String value) {
    Assert.hasText(value, "value is required");
    return value.trim().replaceAll(REGEX_STRING_BEFORE_SLASH, BLANK);
  }

  public static String[] distinctInRight(final String[] left, final String[] right) {
    Assert.notEmpty(left, "left is required");
    Assert.notEmpty(right, "right is required");
    return Arrays.stream(right)
        .filter(col -> !ArrayUtils.contains(left, col))
        .distinct()
        .toArray(String[]::new);
  }
}

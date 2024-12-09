package com.ksoot.spark.common.util;

import jakarta.validation.constraints.Size;
import java.time.Duration;
import java.util.ArrayList;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.time.DateUtils;

/**
 * @author Rajveer Singh
 */
@AllArgsConstructor(staticName = "of")
public class DurationRepresentation {

  @Size(min = 0)
  private long hours;

  @Size(min = 0, max = 59)
  private long minutes;

  @Size(min = 0, max = 59)
  private long seconds;

  @Size(min = 0, max = 999)
  private long milliSeconds;

  public static DurationRepresentation of(final Duration duration) {
    return Factory.frequencyOf(duration);
  }

  // public static void main(String[] args) {
  // Duration duration = Duration.ofHours(3).plusMinutes(34)
  // .plusSeconds(23).plusMillis(113);
  // DurationRepresentation frequency = DurationRepresentation.of(duration);
  // System.out.println(frequency);
  // }

  public Duration duration() {
    Duration duration = Duration.ofHours(this.hours);
    duration = duration.plusMinutes(this.minutes);
    duration = duration.plusSeconds(this.seconds);
    duration = duration.plusMillis(this.milliSeconds);
    return duration;
  }

  @Override
  public String toString() {
    // return String.format("%02d:%02d:%02d:%03d", this.hours, this.minutes,
    // this.seconds, this.milliSeconds);
    // return String.format("%02d Hours %02d Minutes %02d Seconds %03d Milli Seconds",
    // this.hours, this.minutes, this.seconds, this.milliSeconds);

    return (this.hours != 0 ? this.hours + " Hours " : "")
        + (this.minutes != 0 ? this.minutes + " Minutes " : "")
        + (this.seconds != 0 ? this.seconds + " Seconds " : "")
        + (this.milliSeconds != 0 ? this.milliSeconds + " Milli Seconds" : "");
  }

  private static final class Factory {

    public static String ISO_EXTENDED_FORMAT_PATTERN = "'P'yyyy'Y'M'M'd'DT'H'H'm'M's.SSS'S'";

    static final Object y = "y";

    static final Object M = "M";

    static final Object d = "d";

    static final Object H = "H";

    static final Object m = "m";

    static final Object s = "s";

    static final Object S = "S";

    public static DurationRepresentation frequencyOf(final Duration duration) {
      return frequencyOf(duration.toMillis(), ISO_EXTENDED_FORMAT_PATTERN);
    }

    private static DurationRepresentation frequencyOf(
        final long durationMillis, final String pattern) {
      Validate.inclusiveBetween(
          0, Long.MAX_VALUE, durationMillis, "durationMillis must not be negative");

      final Token[] tokens = lexx(pattern);

      long hours = 0;
      long minutes = 0;
      long seconds = 0;
      long milliseconds = durationMillis;

      if (Token.containsTokenWithValue(tokens, H)) {
        hours = milliseconds / DateUtils.MILLIS_PER_HOUR;
        milliseconds = milliseconds - (hours * DateUtils.MILLIS_PER_HOUR);
      }
      if (Token.containsTokenWithValue(tokens, m)) {
        minutes = milliseconds / DateUtils.MILLIS_PER_MINUTE;
        milliseconds = milliseconds - (minutes * DateUtils.MILLIS_PER_MINUTE);
      }
      if (Token.containsTokenWithValue(tokens, s)) {
        seconds = milliseconds / DateUtils.MILLIS_PER_SECOND;
        milliseconds = milliseconds - (seconds * DateUtils.MILLIS_PER_SECOND);
      }

      return DurationRepresentation.of(hours, minutes, seconds, milliseconds);
    }

    static Token[] lexx(final String format) {
      final ArrayList<Token> list = new ArrayList<>(format.length());

      boolean inLiteral = false;
      // Although the buffer is stored in a Token, the Tokens are only
      // used internally, so cannot be accessed by other threads
      StringBuilder buffer = null;
      Token previous = null;
      for (int i = 0; i < format.length(); i++) {
        final char ch = format.charAt(i);
        if (inLiteral && ch != '\'') {
          buffer.append(ch); // buffer can't be null if inLiteral is true
          continue;
        }
        Object value = null;
        switch (ch) {
          // TODO: Need to handle escaping of '
          case '\'':
            if (inLiteral) {
              buffer = null;
              inLiteral = false;
            } else {
              buffer = new StringBuilder();
              list.add(new Token(buffer));
              inLiteral = true;
            }
            break;
          case 'y':
            value = y;
            break;
          case 'M':
            value = M;
            break;
          case 'd':
            value = d;
            break;
          case 'H':
            value = H;
            break;
          case 'm':
            value = m;
            break;
          case 's':
            value = s;
            break;
          case 'S':
            value = S;
            break;
          default:
            if (buffer == null) {
              buffer = new StringBuilder();
              list.add(new Token(buffer));
            }
            buffer.append(ch);
        }

        if (value != null) {
          if (previous != null && previous.getValue().equals(value)) {
            previous.increment();
          } else {
            final Token token = new Token(value);
            list.add(token);
            previous = token;
          }
          buffer = null;
        }
      }
      if (inLiteral) { // i.e. we have not found the end of the literal
        throw new IllegalArgumentException("Unmatched quote in format: " + format);
      }
      return list.toArray(new Token[list.size()]);
    }

    // -----------------------------------------------------------------------
    /** Element that is parsed from the format pattern. */
    static class Token {

      /**
       * Helper method to determine if a set of tokens contain a value
       *
       * @param tokens set to look in
       * @param value to look for
       * @return boolean <code>true</code> if contained
       */
      static boolean containsTokenWithValue(final Token[] tokens, final Object value) {
        for (final Token token : tokens) {
          if (token.getValue() == value) {
            return true;
          }
        }
        return false;
      }

      private final Object value;

      private int count;

      /**
       * Wraps a token around a value. A value would be something like a 'Y'.
       *
       * @param value to wrap
       */
      Token(final Object value) {
        this.value = value;
        this.count = 1;
      }

      /** Adds another one of the value */
      void increment() {
        count++;
      }

      /**
       * Gets the particular value this token represents.
       *
       * @return Object value
       */
      Object getValue() {
        return value;
      }

      /**
       * Supports equality of this Token to another Token.
       *
       * @param obj2 Object to consider equality of
       * @return boolean <code>true</code> if equal
       */
      @Override
      public boolean equals(final Object obj2) {
        if (obj2 instanceof Token) {
          final Token tok2 = (Token) obj2;
          if (this.value.getClass() != tok2.value.getClass()) {
            return false;
          }
          if (this.count != tok2.count) {
            return false;
          }
          if (this.value instanceof StringBuilder) {
            return this.value.toString().equals(tok2.value.toString());
          } else if (this.value instanceof Number) {
            return this.value.equals(tok2.value);
          } else {
            return this.value == tok2.value;
          }
        }
        return false;
      }

      /**
       * Returns a hash code for the token equal to the hash code for the token's value. Thus 'TT'
       * and 'TTTT' will have the same hash code.
       *
       * @return The hash code for the token
       */
      @Override
      public int hashCode() {
        return this.value.hashCode();
      }

      /**
       * Represents this token as a String.
       *
       * @return String representation of the token
       */
      @Override
      public String toString() {
        return StringUtils.repeat(this.value.toString(), this.count);
      }
    }
  }
}

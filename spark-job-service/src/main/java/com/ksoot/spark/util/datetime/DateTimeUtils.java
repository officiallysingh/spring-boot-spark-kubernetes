package com.ksoot.spark.util.datetime;

import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.Locale;
import lombok.experimental.UtilityClass;

@UtilityClass
public class DateTimeUtils {

  public static final ZoneId ZONE_ID_IST = ZoneId.of("Asia/Kolkata");
  public static final ZoneId ZONE_ID_UTC = ZoneId.of("UTC");
  public static final ZoneOffset ZONE_OFFSET_IST = ZoneOffset.of("+05:30");
  public static final ZoneId SYSTEM_ZONE_ID = ZoneId.systemDefault();
  public static final Locale SYSTEM_LOCALE = Locale.getDefault();
  public static final String SYSTEM_ZONE_DISPLAY_NAME =
      SYSTEM_ZONE_ID.getDisplayName(TextStyle.FULL, SYSTEM_LOCALE)
          + "("
          + SYSTEM_ZONE_ID.getDisplayName(TextStyle.SHORT, SYSTEM_LOCALE)
          + ")";

  public static LocalDateTime fromISTtoUTC(final LocalDateTime localDateTimeIST) {
    // Convert LocalDateTime from IST to ZonedDateTime
    final ZonedDateTime zonedDateTimeIST = localDateTimeIST.atZone(ZONE_ID_IST);
    // Convert ZonedDateTime to UTC
    final ZonedDateTime zonedDateTimeUTC = zonedDateTimeIST.withZoneSameInstant(ZONE_ID_UTC);
    return zonedDateTimeUTC.toLocalDateTime();
  }

  public static LocalDateTime fromUTCtoIST(final LocalDateTime localDateTimeUTC) {
    // Convert LocalDateTime from UTC to ZonedDateTime
    final ZonedDateTime zonedDateTimeIST = localDateTimeUTC.atZone(ZONE_ID_UTC);
    // Convert ZonedDateTime to IST
    final ZonedDateTime zonedDateTimeUTC = zonedDateTimeIST.withZoneSameInstant(ZONE_ID_IST);
    return zonedDateTimeUTC.toLocalDateTime();
  }

  public static LocalDate nowLocalDateIST() {
    return isSystemTimeZoneIST()
        ? LocalDate.now()
        : fromUTCtoIST(LocalDateTime.now()).toLocalDate();
  }

  public static YearMonth currentMonthIST() {
    return YearMonth.from(nowLocalDateIST());
  }

  public static YearMonth previousMonthIST() {
    return currentMonthIST().minusMonths(1);
  }

  public static ZonedDateTime monthStartZonedDateTime(final Month month, final ZoneId zoneId) {
    return LocalDate.of(nowLocalDateIST().getYear(), month, 1).atStartOfDay().atZone(zoneId);
  }

  public static ZonedDateTime monthEndZonedDateTime(final Month month, final ZoneId zoneId) {
    return LocalDate.of(nowLocalDateIST().getYear(), month, 1)
        .with(TemporalAdjusters.lastDayOfMonth())
        .plusDays(1)
        .atStartOfDay()
        .minusSeconds(1)
        .atZone(zoneId);
  }

  public static ZonedDateTime monthStartZonedDateTimeIST(final Month month) {
    return monthStartZonedDateTime(month, ZONE_ID_IST);
  }

  public static ZonedDateTime monthEndZonedDateTimeIST(final Month month) {
    return monthEndZonedDateTime(month, ZONE_ID_IST);
  }

  public static ZonedDateTime toZonedDateTimeIST(final Date date) {
    return toZonedDateTime(date, ZONE_ID_IST);
  }

  public static ZonedDateTime toZonedDateTime(final Date date, final ZoneId zoneId) {
    return date.toInstant().atZone(zoneId);
  }

  public static LocalDateTime trimTime(final LocalDateTime localDateTime) {
    return LocalDateTime.of(localDateTime.toLocalDate(), LocalTime.MIDNIGHT);
  }

  public static ZonedDateTime trimTime(final ZonedDateTime zonedDateTime) {
    return zonedDateTime.withHour(0).withMinute(0).withSecond(0).withNano(0);
  }

  public static boolean isSystemTimeZoneUTC() {
    return SYSTEM_ZONE_ID.equals(ZONE_ID_UTC);
  }

  public static boolean isSystemTimeZoneIST() {
    return SYSTEM_ZONE_ID.equals(ZONE_ID_IST);
  }
}

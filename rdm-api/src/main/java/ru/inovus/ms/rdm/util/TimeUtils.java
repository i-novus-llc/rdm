package ru.inovus.ms.rdm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.util.Objects.isNull;

public final class TimeUtils {

    public static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_PATTERN_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
    public static final String DATE_PATTERN_WITH_POINT = "dd.MM.yyyy";
    public static final DateTimeFormatter DATE_PATTERN_WITH_POINT_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_POINT);
    public static final String DATE_PATTERN_WITH_HYPHEN = "yyyy-MM-dd";
    public static final DateTimeFormatter DATE_PATTERN_WITH_HYPHEN_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_HYPHEN);

    private static final ZoneId LOCALIZED_TIMEZONE_ID = ZoneId.of("Europe/Moscow");
    private static final ZoneId UNIVERSAL_TIMEZONE_ID = ZoneId.of("UTC");

    private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);

    private TimeUtils() {
    }

    public static OffsetDateTime parseOffsetDateTime(String str) {
        try {
            return LocalDateTime.parse(str, DATE_TIME_PATTERN_FORMATTER).atOffset(ZoneOffset.UTC);
        } catch (DateTimeException e) {
            logger.debug("Failed to parse Date&Time using format: " + DATE_TIME_PATTERN, e);
        }

        throw new IllegalArgumentException("Failed to parse Date&Time: " + str);
    }

    public static LocalDateTime parseLocalDateTime(String str) {
        return parseOffsetDateTime(str).toLocalDateTime();
    }

    public static String format(LocalDateTime localDateTime) {
        return localDateTime.format(DATE_TIME_PATTERN_FORMATTER);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(UNIVERSAL_TIMEZONE_ID);
    }

    public static LocalDateTime nowZoned() {
        return LocalDateTime.now(LOCALIZED_TIMEZONE_ID);
    }

    private static LocalDateTime convert(LocalDateTime localDateTime, ZoneId fromZone, ZoneId toZone) {
        return localDateTime.atZone(fromZone).withZoneSameInstant(toZone).toLocalDateTime();
    }

    public static LocalDateTime zonedToUtc(LocalDateTime localDateTime) {
        return convert(localDateTime, LOCALIZED_TIMEZONE_ID, UNIVERSAL_TIMEZONE_ID);
    }

    public static LocalDateTime utcToZoned(LocalDateTime localDateTime) {
        return convert(localDateTime, UNIVERSAL_TIMEZONE_ID, LOCALIZED_TIMEZONE_ID);
    }

    public static LocalDate parseLocalDate(Object value) {
        if (value instanceof Date)
            return ((Date) value).toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        if (value instanceof LocalDate)
            return (LocalDate) value;
        if (value instanceof LocalDateTime)
            return ((LocalDateTime) value).toLocalDate();
        return LocalDate.parse(
                String.valueOf(value),
                String.valueOf(value).contains(".")
                        ? DATE_PATTERN_WITH_POINT_FORMATTER
                        : DATE_PATTERN_WITH_HYPHEN_FORMATTER
        );
    }

    public static String format(LocalDate localDate) {
        return localDate.format(DATE_PATTERN_WITH_POINT_FORMATTER);
    }

    public static String format(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DATE_TIME_PATTERN_FORMATTER);
    }

    public static boolean isSameOrBeforeNow(LocalDateTime localDateTime) {
        LocalDateTime now = now();
        return !isNull(localDateTime) && (localDateTime.equals(now) || localDateTime.isBefore(now));
    }

    public static boolean isNullOrAfterNow(LocalDateTime localDateTime) {
        return isNull(localDateTime) || localDateTime.isAfter(now());
    }
}

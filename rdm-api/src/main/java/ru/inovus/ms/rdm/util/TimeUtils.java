package ru.inovus.ms.rdm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;

import static java.util.Objects.isNull;

public final class TimeUtils {

    public static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_PATTERN_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

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

    public static String format(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DATE_TIME_PATTERN_FORMATTER);
    }

    public static boolean isSameOrBeforeNow(LocalDateTime localDateTime) {
        LocalDateTime now = LocalDateTime.now();
        return !isNull(localDateTime) && (localDateTime.equals(now) || localDateTime.isBefore(now));
    }

    public static boolean isNullOrAfterNow(LocalDateTime localDateTime) {
        return isNull(localDateTime) || localDateTime.isAfter(LocalDateTime.now());
    }
}

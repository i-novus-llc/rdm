package ru.inovus.ms.rdm.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;

public final class TimeUtils {

    private TimeUtils() {
    }

    public static final String DATE_TIME_PATTERN = "dd.MM.yyyy HH:mm:ss";
    public static final DateTimeFormatter DATE_TIME_PATTERN_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);

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
}

package ru.inovus.ms.rdm.n2o.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import static java.util.Objects.isNull;

public final class TimeUtils {

    public static final String DATE_TIME_PATTERN_ISO = "yyyy-MM-dd HH:mm:ss";
    public static final String DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String DATE_TIME_PATTERN_ISO_WITH_MICROSEC_DELIMITER = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS";
    public static final String DATE_TIME_PATTERN_ISO_WITH_MILLISEC_DELIMITER = "yyyy-MM-dd'T'HH:mm:ss.SSS";
    public static final String DATE_TIME_PATTERN_ISO_WITH_CENTSEC_DELIMITER = "yyyy-MM-dd'T'HH:mm:ss.SS";
    public static final String DATE_TIME_PATTERN_EUROPEAN = "dd.MM.yyyy HH:mm:ss";

    public static final String DATE_PATTERN_ISO = "yyyy-MM-dd";
    public static final String DATE_PATTERN_EUROPEAN = "dd.MM.yyyy";

    public static final String DATE_TIME_PATTERN_ISO_REGEX = "^(\\d{4})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01]) (0?[0-9]|[1][0-9]|2[0-3]):(0?[0-9]|[1-5][0-9]):(0?[0-9]|[1-5][0-9])$";
    public static final String DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_REGEX = "^(\\d{4})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])T(0?[0-9]|[1][0-9]|2[0-3]):(0?[0-9]|[1-5][0-9]):(0?[0-9]|[1-5][0-9])$";
    public static final String DATE_TIME_PATTERN_ISO_WITH_MICROSEC_DELIMITER_REGEX = "^(\\d{4})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])T(0?[0-9]|[1][0-9]|2[0-3]):(0?[0-9]|[1-5][0-9]):(0?[0-9]|[1-5][0-9]).([0-9][0-9][0-9][0-9][0-9][0-9])$";
    public static final String DATE_TIME_PATTERN_ISO_WITH_MILLISEC_DELIMITER_REGEX = "^(\\d{4})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])T(0?[0-9]|[1][0-9]|2[0-3]):(0?[0-9]|[1-5][0-9]):(0?[0-9]|[1-5][0-9]).([0-9][0-9][0-9])$";
    public static final String DATE_TIME_PATTERN_ISO_WITH_CENTSEC_DELIMITER_REGEX = "^(\\d{4})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])T(0?[0-9]|[1][0-9]|2[0-3]):(0?[0-9]|[1-5][0-9]):(0?[0-9]|[1-5][0-9]).([0-9][0-9])$";
    public static final String DATE_TIME_PATTERN_EUROPEAN_REGEX = "^(0?[1-9]|[12][0-9]|3[01]).(0?[1-9]|1[012]).(\\d{4}) (0?[0-9]|[1][0-9]|2[0-3]):(0?[0-9]|[1-5][0-9]):(0?[0-9]|[1-5][0-9])$";
    public static final String DATE_PATTERN_ISO_REGEX = "^(\\d{4})-(0?[1-9]|1[012])-(0?[1-9]|[12][0-9]|3[01])$";
    public static final String DATE_PATTERN_EUROPEAN_REGEX = "^(0?[1-9]|[12][0-9]|3[01]).(0?[1-9]|1[012]).(\\d{4})$";

    public static final DateTimeFormatter DATE_TIME_PATTERN_ISO_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_ISO);
    public static final DateTimeFormatter DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER);
    public static final DateTimeFormatter DATE_TIME_PATTERN_ISO_WITH_MICROSEC_DELIMITER_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_ISO_WITH_MICROSEC_DELIMITER);
    public static final DateTimeFormatter DATE_TIME_PATTERN_ISO_WITH_MILLISEC_DELIMITER_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_ISO_WITH_MILLISEC_DELIMITER);
    public static final DateTimeFormatter DATE_TIME_PATTERN_ISO_WITH_CENTSEC_DELIMITER_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_ISO_WITH_CENTSEC_DELIMITER);
    public static final DateTimeFormatter DATE_TIME_PATTERN_EUROPEAN_FORMATTER = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN_EUROPEAN);
    public static final DateTimeFormatter DATE_PATTERN_ISO_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN_ISO);
    public static final DateTimeFormatter DATE_PATTERN_EUROPEAN_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN_EUROPEAN);

    private static final ZoneId LOCALIZED_TIMEZONE = ZoneId.of("Europe/Moscow");
    private static final ZoneId UNIVERSAL_TIMEZONE = ZoneId.of("UTC");

    private static final Logger logger = LoggerFactory.getLogger(TimeUtils.class);

    private TimeUtils() {
    }

    public static OffsetDateTime parseOffsetDateTime(String str) {

        try {

            if (str.matches(DATE_TIME_PATTERN_ISO_REGEX)) {
                return LocalDateTime.parse(str, DATE_TIME_PATTERN_ISO_FORMATTER).atOffset(ZoneOffset.UTC);
            }
            if (str.matches(DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_REGEX)) {
                return LocalDateTime.parse(str, DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_FORMATTER).atOffset(ZoneOffset.UTC);
            }
            if (str.matches(DATE_TIME_PATTERN_ISO_WITH_MICROSEC_DELIMITER_REGEX)) {
                return LocalDateTime.parse(str, DATE_TIME_PATTERN_ISO_WITH_MICROSEC_DELIMITER_FORMATTER).atOffset(ZoneOffset.UTC);
            }
            if (str.matches(DATE_TIME_PATTERN_ISO_WITH_MILLISEC_DELIMITER_REGEX)) {
                return LocalDateTime.parse(str, DATE_TIME_PATTERN_ISO_WITH_MILLISEC_DELIMITER_FORMATTER).atOffset(ZoneOffset.UTC);
            }
            if (str.matches(DATE_TIME_PATTERN_ISO_WITH_CENTSEC_DELIMITER_REGEX)) {
                return LocalDateTime.parse(str, DATE_TIME_PATTERN_ISO_WITH_CENTSEC_DELIMITER_FORMATTER).atOffset(ZoneOffset.UTC);
            }
            if (str.matches(DATE_TIME_PATTERN_EUROPEAN_REGEX)) {
                return LocalDateTime.parse(str, DATE_TIME_PATTERN_EUROPEAN_FORMATTER).atOffset(ZoneOffset.UTC);
            }

            if (str.matches(DATE_PATTERN_ISO_REGEX)) {
                return LocalDateTime.of(LocalDate.parse(str, DATE_PATTERN_ISO_FORMATTER), LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC);
            }
            if (str.matches(DATE_PATTERN_EUROPEAN_REGEX)) {
                return LocalDateTime.of(LocalDate.parse(str, DATE_PATTERN_EUROPEAN_FORMATTER), LocalTime.MIDNIGHT).atOffset(ZoneOffset.UTC);
            }

            throw new IllegalArgumentException("Failed to parse Date&Time: " + str);

        } catch (DateTimeException e) {
            logger.debug("Failed to parse Date&Time " + str, e);
            throw new IllegalArgumentException("Failed to parse Date&Time: " + str);
        }
    }


    public static LocalDateTime parseLocalDateTime(String str) {
        return parseOffsetDateTime(str).toLocalDateTime();
    }

    public static String format(LocalDateTime localDateTime) {
        return localDateTime.format(DATE_TIME_PATTERN_ISO_WITH_TIME_DELIMITER_FORMATTER);
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(UNIVERSAL_TIMEZONE);
    }

    public static LocalDateTime nowZoned() {
        return LocalDateTime.now(LOCALIZED_TIMEZONE);
    }

    /**
     * Convert datetime from one zone to another.
     * Преобразование даты-времени из одной зоны в другую.
     *
     * @param localDateTime исходное значение даты-времени
     * @param fromZone      исходная зона
     * @param toZone        требуемая зона
     * @return Преобразованное значение даты-времени
     */
    private static LocalDateTime convert(LocalDateTime localDateTime, ZoneId fromZone, ZoneId toZone) {
        return (localDateTime != null) ? localDateTime.atZone(fromZone).withZoneSameInstant(toZone).toLocalDateTime() : null;
    }

    public static LocalDateTime zonedToUtc(LocalDateTime localDateTime) {
        return convert(localDateTime, LOCALIZED_TIMEZONE, UNIVERSAL_TIMEZONE);
    }

    public static LocalDateTime utcToZoned(LocalDateTime localDateTime) {
        return convert(localDateTime, UNIVERSAL_TIMEZONE, LOCALIZED_TIMEZONE);
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
                        ? DATE_PATTERN_EUROPEAN_FORMATTER
                        : DATE_PATTERN_ISO_FORMATTER
        );
    }

    public static String format(LocalDate localDate) {
        return localDate.format(DATE_PATTERN_EUROPEAN_FORMATTER);
    }

    public static String format(OffsetDateTime offsetDateTime) {
        return offsetDateTime.format(DATE_TIME_PATTERN_EUROPEAN_FORMATTER);
    }

    public static boolean isSameOrBeforeNow(LocalDateTime localDateTime) {
        LocalDateTime now = now();
        return !isNull(localDateTime) && (localDateTime.equals(now) || localDateTime.isBefore(now));
    }

    public static boolean isNullOrAfterNow(LocalDateTime localDateTime) {
        return isNull(localDateTime) || localDateTime.isAfter(now());
    }
}

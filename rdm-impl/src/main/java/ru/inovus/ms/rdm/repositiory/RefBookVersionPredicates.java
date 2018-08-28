package ru.inovus.ms.rdm.repositiory;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import ru.inovus.ms.rdm.entity.QPassportValueEntity;
import ru.inovus.ms.rdm.entity.QRefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TimeZone;

public final class RefBookVersionPredicates {
    public static final LocalDateTime MAX_TIMESTAMP = LocalDateTime.ofInstant(Instant.ofEpochMilli(Integer.MAX_VALUE * 1000L),
            TimeZone.getDefault().toZoneId());
    public static final LocalDateTime MIN_TIMESTAMP = LocalDateTime.ofInstant(Instant.ofEpochMilli(0L),
            TimeZone.getDefault().toZoneId());

    private RefBookVersionPredicates() {
    }

    public static BooleanExpression isVersionOfRefBook(Integer refBookId) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.id.eq(refBookId);
    }

    public static BooleanExpression hasVersionId(Integer versionId) {
        return QRefBookVersionEntity.refBookVersionEntity.id.eq(versionId);
    }

    public static BooleanExpression isPublished() {
        return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.PUBLISHED);
    }

    public static BooleanExpression isAnyPublished() {
        QRefBookVersionEntity anyVersion = QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();
        return anyVersion.status.eq(RefBookVersionStatus.PUBLISHED);
    }

    public static BooleanExpression isPublishing() {
        return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.PUBLISHING);
    }

    public static BooleanExpression isDraft() {
        return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.DRAFT);
    }

    public static BooleanExpression isArchived() {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.archived.isTrue();
    }

    public static BooleanExpression isRemovable() {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.removable.isTrue();
    }

    public static BooleanExpression isLast() {
        QRefBookVersionEntity qEntity = QRefBookVersionEntity.refBookVersionEntity;

        return isDraft().
                or(qEntity.refBook.versionList.any().status.eq(RefBookVersionStatus.DRAFT).not()
                        .and(qEntity.toDate.isNull()));
    }

    public static BooleanExpression isLastPublished() {
        return isPublished().and(QRefBookVersionEntity.refBookVersionEntity.toDate.isNull());
    }

    public static BooleanExpression isCodeContains(String code) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.code.containsIgnoreCase(code.trim());
    }

    public static BooleanExpression isVersionNumberContains(String version) {
        return QRefBookVersionEntity.refBookVersionEntity.version.containsIgnoreCase(version.trim());
    }

    public static BooleanExpression isMaxFromDateEqOrAfter(LocalDateTime dateTime) {
        QRefBookVersionEntity anyVersion = QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();
        return anyVersion.fromDate.eq(dateTime).or(anyVersion.fromDate.after(dateTime));
    }

    public static BooleanExpression isMaxFromDateEqOrBefore(LocalDateTime dateTime) {
        QRefBookVersionEntity anyVersion = QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();

        return anyVersion.fromDate.eq(dateTime).or(anyVersion.fromDate.before(dateTime))
                .and(anyVersion.fromDate.after(dateTime).not());
    }

    public static BooleanExpression hasOverlappingPeriods(LocalDateTime fromDate, LocalDateTime toDate) {

        return QRefBookVersionEntity.refBookVersionEntity.fromDate.coalesce(MIN_TIMESTAMP).asDateTime().before(toDate)
                .and(QRefBookVersionEntity.refBookVersionEntity.toDate.coalesce(MAX_TIMESTAMP).asDateTime().after(fromDate));
    }

    public static BooleanExpression hasOverlappingPeriodsInFuture(LocalDateTime fromDate, LocalDateTime toDate, LocalDateTime now) {

        if (fromDate == null || fromDate.isBefore(now)) {
            fromDate = now;
        }

        if (toDate != null && toDate.isAfter(now)) {
            return QRefBookVersionEntity.refBookVersionEntity.fromDate.coalesce(LocalDateTime.MIN).asDateTime().before(toDate)
                    .and(QRefBookVersionEntity.refBookVersionEntity.toDate.coalesce(LocalDateTime.MAX).asDateTime().after(fromDate))
                    .and(QRefBookVersionEntity.refBookVersionEntity.toDate.coalesce(LocalDateTime.MAX).asDateTime().after(now));
        } else {
            return Expressions.asBoolean(true).isFalse();
        }
    }

    public static BooleanExpression hasAttributeValue(String attribute, String value) {
        return JPAExpressions.selectFrom(QPassportValueEntity.passportValueEntity).where(
                QPassportValueEntity.passportValueEntity.attribute.code.eq(attribute)
                        .and(QPassportValueEntity.passportValueEntity.value.containsIgnoreCase(value))
                        .and(QPassportValueEntity.passportValueEntity.version.eq(QRefBookVersionEntity.refBookVersionEntity))).exists();
    }
}

package ru.inovus.ms.rdm.impl.predicate;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import ru.inovus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.impl.entity.QPassportValueEntity;
import ru.inovus.ms.rdm.impl.entity.QRefBookVersionEntity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.TimeZone;

@SuppressWarnings("WeakerAccess")
public final class RefBookVersionPredicates {

    public static final LocalDateTime MAX_TIMESTAMP = LocalDateTime.ofInstant(Instant.ofEpochMilli(Integer.MAX_VALUE * 1000L),
            TimeZone.getDefault().toZoneId());
    public static final LocalDateTime MIN_TIMESTAMP = LocalDateTime.ofInstant(Instant.ofEpochMilli(0L),
            TimeZone.getDefault().toZoneId());

    private static final String WHERE_EXISTS_VERSION = "existsVersion";
    private static final String WHERE_IS_LAST_DATE_VERSION = "isLastDateVersion";

    private RefBookVersionPredicates() {
    }

    public static BooleanExpression isVersionOfRefBook(Integer refBookId) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.id.eq(refBookId);
    }

    public static BooleanExpression isVersionOfRefBookCode(String refBookCode) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.code.eq(refBookCode);
    }

    public static BooleanExpression isVersionOfRefBooks(List<Integer> refBookIds) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.id.in(refBookIds);
    }

    public static BooleanExpression isCodeEquals(String code) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.code.eq(code.trim());
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

    public static BooleanExpression isSourceType(RefBookSourceType sourceType) {
        if (sourceType == null)
            return isLastVersion();

        switch (sourceType) {
            case ALL: return null;
            case ACTUAL: return isActual();
            case DRAFT: return isDraft();
            case LAST_PUBLISHED: return isLastPublished();
            case LAST_VERSION: return isLastVersion();

            default:
                throw new RdmException("unknown.refbook.source.type");
        }
    }

    private static BooleanExpression isActual() {
        LocalDateTime now = LocalDateTime.now();
        return isPublished().and(
                QRefBookVersionEntity.refBookVersionEntity.fromDate.loe(now).and(
                        QRefBookVersionEntity.refBookVersionEntity.toDate.after(now).or(
                                QRefBookVersionEntity.refBookVersionEntity.toDate.isNull())));
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

    private static BooleanExpression isLastPublished() {
        QRefBookVersionEntity whereVersion = new QRefBookVersionEntity(WHERE_IS_LAST_DATE_VERSION);
        return isPublished().and(
                QRefBookVersionEntity.refBookVersionEntity.fromDate
                        .eq(JPAExpressions
                                .select(whereVersion.fromDate.max()).from(whereVersion)
                                .where(whereVersion.refBook.eq(QRefBookVersionEntity.refBookVersionEntity.refBook))));
    }

    private static BooleanExpression isLastVersion() {
        return refBookHasDraft().not().and(isLastPublished()).or(isDraft());
    }

    public static BooleanExpression refBookHasCategory(String category) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.category.eq(category);
    }

    public static BooleanExpression isPublished() {
        return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.PUBLISHED);
    }

    public static BooleanExpression refBookHasVersion(Integer versionId) {
        QRefBookVersionEntity anyVersion = QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();
        return anyVersion.id.eq(versionId);
    }

    public static BooleanExpression refBookHasDraft() {
        QRefBookVersionEntity anyVersion = QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();
        return anyVersion.status.eq(RefBookVersionStatus.DRAFT);
    }

    public static BooleanExpression refBookHasPublished() {
        QRefBookVersionEntity anyVersion = QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();
        return anyVersion.status.eq(RefBookVersionStatus.PUBLISHED);
    }

    public static BooleanExpression hasStructure() {
        return QRefBookVersionEntity.refBookVersionEntity.structure.isNotNull();
    }

    // NB: hasPrimaryAttribute требует серьёзной доработки для проверки isPrimary в атрибутах из jsonb-поля.
    public static BooleanExpression hasPrimaryAttribute() {
        QRefBookVersionEntity fieldVersion = new QRefBookVersionEntity(WHERE_EXISTS_VERSION);
        QRefBookVersionEntity whereVersion = new QRefBookVersionEntity(WHERE_IS_LAST_DATE_VERSION);
        return JPAExpressions
                .select(fieldVersion.version).from(fieldVersion)
                .where(fieldVersion.refBook.eq(QRefBookVersionEntity.refBookVersionEntity.refBook)
                    .and(fieldVersion.status.eq(RefBookVersionStatus.PUBLISHED))
                    .and(fieldVersion.fromDate
                            .eq(JPAExpressions
                                    .select(whereVersion.fromDate.max()).from(whereVersion)
                                    .where(whereVersion.refBook.eq(fieldVersion.refBook)
                                            .and(whereVersion.status.eq(RefBookVersionStatus.PUBLISHED)))
                            ))
                    // NB: Реализовать проверку на наличие первичного ключа
                    .and(fieldVersion.structure.isNotNull())
                ).exists();
    }

    public static BooleanExpression isDisplayCodeContains(String displayCode) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.code.containsIgnoreCase(displayCode.trim())
                .or(hasPassportAttributeValue("name", displayCode.trim()));
    }

    public static BooleanExpression hasVersionId(Integer versionId) {
        return QRefBookVersionEntity.refBookVersionEntity.id.eq(versionId);
    }

    public static BooleanExpression isVersionNumberContains(String version) {
        return QRefBookVersionEntity.refBookVersionEntity.version.containsIgnoreCase(version.trim());
    }

    public static BooleanExpression hasOverlappingPeriods(LocalDateTime fromDate, LocalDateTime toDate) {

        return QRefBookVersionEntity.refBookVersionEntity.fromDate.coalesce(MIN_TIMESTAMP).asDateTime().before(toDate)
                .and(QRefBookVersionEntity.refBookVersionEntity.toDate.coalesce(MAX_TIMESTAMP).asDateTime().after(fromDate));
    }

    @SuppressWarnings("unused")
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

    public static BooleanExpression hasPassportAttributeValue(String attribute, String value) {
        return JPAExpressions.selectFrom(QPassportValueEntity.passportValueEntity).where(
                QPassportValueEntity.passportValueEntity.attribute.code.eq(attribute)
                        .and(QPassportValueEntity.passportValueEntity.value.containsIgnoreCase(value))
                        .and(QPassportValueEntity.passportValueEntity.version.eq(QRefBookVersionEntity.refBookVersionEntity))).exists();
    }
}

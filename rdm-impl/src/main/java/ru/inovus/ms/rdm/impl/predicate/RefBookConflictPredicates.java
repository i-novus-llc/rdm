package ru.inovus.ms.rdm.impl.predicate;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import ru.inovus.ms.rdm.impl.entity.QRefBookConflictEntity;
import ru.inovus.ms.rdm.impl.entity.QRefBookVersionEntity;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public final class RefBookConflictPredicates {

    private static final String WHERE_IS_LAST_DATE_VERSION = "isLastDateVersion";

    private RefBookConflictPredicates() {
    }

    public static BooleanExpression isReferrerVersionId(Integer referrerVersionId) {
        return QRefBookConflictEntity.refBookConflictEntity.referrerVersion.id.eq(referrerVersionId);
    }

    public static BooleanExpression isReferrerVersionRefBookId(Integer referrerVersionRefBookId) {
        return QRefBookConflictEntity.refBookConflictEntity.referrerVersion.refBook.id.eq(referrerVersionRefBookId);
    }

    public static BooleanExpression isPublishedVersionRefBookId(Integer publishedVersionRefBookId) {
        return QRefBookConflictEntity.refBookConflictEntity.publishedVersion.refBook.id.eq(publishedVersionRefBookId);
    }

    public static BooleanExpression isPublishedVersionId(Integer publishedVersionId) {
        return QRefBookConflictEntity.refBookConflictEntity.publishedVersion.id.eq(publishedVersionId);
    }

    public static BooleanExpression isRefRecordId(Long refRecordId) {
        return QRefBookConflictEntity.refBookConflictEntity.refRecordId.eq(refRecordId);
    }

    public static BooleanExpression isRefRecordIdIn(List<Long> refRecordIds) {
        return QRefBookConflictEntity.refBookConflictEntity.refRecordId.in(refRecordIds);
    }

    public static BooleanExpression isRefFieldCode(String refFieldCode) {
        return QRefBookConflictEntity.refBookConflictEntity.refFieldCode.eq(refFieldCode);
    }

    public static BooleanExpression isConflictType(ConflictType conflictType) {
        return QRefBookConflictEntity.refBookConflictEntity.conflictType.eq(conflictType);
    }

    public static BooleanExpression isConflictTypeIn(List<ConflictType> conflictTypes) {
        return QRefBookConflictEntity.refBookConflictEntity.conflictType.in(conflictTypes);
    }

    public static BooleanExpression isLastPublishedVersion() {
        QRefBookVersionEntity whereVersion = new QRefBookVersionEntity(WHERE_IS_LAST_DATE_VERSION);
        return QRefBookConflictEntity.refBookConflictEntity.publishedVersion.fromDate
                .eq(JPAExpressions
                        .select(whereVersion.fromDate.max()).from(whereVersion)
                        .where(whereVersion.refBook
                                .eq(QRefBookConflictEntity.refBookConflictEntity.publishedVersion.refBook)
                                .and(whereVersion.status.eq(RefBookVersionStatus.PUBLISHED)))
                );
    }
}

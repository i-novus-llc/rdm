package ru.inovus.ms.rdm.repositiory;

import com.querydsl.core.types.dsl.BooleanExpression;
import ru.inovus.ms.rdm.entity.QRefBookConflictEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

public final class RefBookConflictPredicates {

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
}

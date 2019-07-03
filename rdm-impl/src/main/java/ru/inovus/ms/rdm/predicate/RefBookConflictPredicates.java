package ru.inovus.ms.rdm.predicate;

import com.querydsl.core.types.dsl.BooleanExpression;
import ru.inovus.ms.rdm.entity.QRefBookConflictEntity;
import ru.inovus.ms.rdm.enumeration.ConflictType;

import java.util.List;

final class RefBookConflictPredicates {

    private RefBookConflictPredicates() {
    }

    static BooleanExpression isReferrerVersionId(Integer referrerVersionId) {
        return QRefBookConflictEntity.refBookConflictEntity.referrerVersion.id.eq(referrerVersionId);
    }

    static BooleanExpression isReferrerVersionRefBookId(Integer referrerVersionRefBookId) {
        return QRefBookConflictEntity.refBookConflictEntity.referrerVersion.refBook.id.eq(referrerVersionRefBookId);
    }

    static BooleanExpression isPublishedVersionRefBookId(Integer publishedVersionRefBookId) {
        return QRefBookConflictEntity.refBookConflictEntity.publishedVersion.refBook.id.eq(publishedVersionRefBookId);
    }

    static BooleanExpression isPublishedVersionId(Integer publishedVersionId) {
        return QRefBookConflictEntity.refBookConflictEntity.publishedVersion.id.eq(publishedVersionId);
    }

    static BooleanExpression isRefRecordId(Long refRecordId) {
        return QRefBookConflictEntity.refBookConflictEntity.refRecordId.eq(refRecordId);
    }

    static BooleanExpression isRefRecordIdIn(List<Long> refRecordIds) {
        return QRefBookConflictEntity.refBookConflictEntity.refRecordId.in(refRecordIds);
    }

    static BooleanExpression isRefFieldCode(String refFieldCode) {
        return QRefBookConflictEntity.refBookConflictEntity.refFieldCode.eq(refFieldCode);
    }

    static BooleanExpression isConflictType(ConflictType conflictType) {
        return QRefBookConflictEntity.refBookConflictEntity.conflictType.eq(conflictType);
    }
}

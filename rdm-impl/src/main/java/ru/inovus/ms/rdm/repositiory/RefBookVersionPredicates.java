package ru.inovus.ms.rdm.repositiory;

import com.querydsl.core.types.dsl.BooleanExpression;
import ru.inovus.ms.rdm.entity.QRefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;

import java.time.LocalDateTime;

public final class RefBookVersionPredicates {

    private RefBookVersionPredicates() {
    }

    public static BooleanExpression isVersionOfRefBook(Integer refBookId) {
        return QRefBookVersionEntity.refBookVersionEntity.refBook.id.eq(refBookId);
    }

    public static BooleanExpression isPublished() {
        return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.PUBLISHED);
    }

    public static BooleanExpression isAnyPublished() {
        QRefBookVersionEntity anyVersion =  QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();
        return anyVersion.status.eq(RefBookVersionStatus.PUBLISHED);
    }

    public static BooleanExpression isPublishing() {
        return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.PUBLISHING);
    }

    public static BooleanExpression isDraft() {
        return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.DRAFT);
    }

    public static BooleanExpression isDraftExcluded(boolean excludeDraft) {
        if (excludeDraft)
            return QRefBookVersionEntity.refBookVersionEntity.status.eq(RefBookVersionStatus.DRAFT).not();
        return null;
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

    public static BooleanExpression isShortNameOrFullNameContains(String name) {
        QRefBookVersionEntity qEntity = QRefBookVersionEntity.refBookVersionEntity;

        return qEntity.refBook.versionList.any().fullName.containsIgnoreCase(name.trim())
                .or(qEntity.refBook.versionList.any().shortName.containsIgnoreCase(name.trim()));
    }

    public static BooleanExpression isMaxFromDateEqOrAfter(LocalDateTime dateTime){
        QRefBookVersionEntity qEntity = QRefBookVersionEntity.refBookVersionEntity;
        return qEntity.refBook.versionList.any().fromDate.eq(dateTime).or(qEntity.fromDate.after(dateTime));
    }

    public static BooleanExpression isMaxFromDateEqOrBefore(LocalDateTime dateTime) {
        QRefBookVersionEntity anyVersion =  QRefBookVersionEntity.refBookVersionEntity.refBook.versionList.any();

        return anyVersion.fromDate.eq(dateTime).or(anyVersion.fromDate.before(dateTime))
                .and(anyVersion.fromDate.after(dateTime).not());
    }
}

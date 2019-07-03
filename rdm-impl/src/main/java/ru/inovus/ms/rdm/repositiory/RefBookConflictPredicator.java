package ru.inovus.ms.rdm.repositiory;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import ru.inovus.ms.rdm.model.conflict.DeleteRefBookConflictCriteria;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;

import static java.util.Objects.nonNull;
import static ru.inovus.ms.rdm.repositiory.RefBookConflictPredicates.*;
import static ru.inovus.ms.rdm.repositiory.RefBookConflictPredicates.isConflictType;

public class RefBookConflictPredicator {

    private RefBookConflictPredicator() {
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    public static Predicate toPredicate(RefBookConflictCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        if (nonNull(criteria.getReferrerVersionId()))
            where.and(isReferrerVersionId(criteria.getReferrerVersionId()));

        if (nonNull(criteria.getReferrerVersionRefBookId()))
            where.and(isReferrerVersionRefBookId(criteria.getReferrerVersionRefBookId()));

        if (nonNull(criteria.getPublishedVersionId()))
            where.and(isPublishedVersionId(criteria.getPublishedVersionId()));

        if (nonNull(criteria.getPublishedVersionRefBookId()))
            where.and(isPublishedVersionRefBookId(criteria.getPublishedVersionRefBookId()));

        if (nonNull(criteria.getRefRecordId()))
            where.and(isRefRecordId(criteria.getRefRecordId()));

        if (nonNull(criteria.getRefRecordIds()))
            where.and(isRefRecordIdIn(criteria.getRefRecordIds()));

        if (nonNull(criteria.getRefFieldCode()))
            where.and(isRefFieldCode(criteria.getRefFieldCode()));

        if (nonNull(criteria.getConflictType()))
            where.and(isConflictType(criteria.getConflictType()));

        return where.getValue();
    }

    /**
     * Формирование предиката на основе критерия удаления.
     *
     * @param criteria критерий удаления
     * @return Предикат для удаления
     */
    public static Predicate toPredicate(DeleteRefBookConflictCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        if (nonNull(criteria.getReferrerVersionId()))
            where.and(isReferrerVersionId(criteria.getReferrerVersionId()));

        if (nonNull(criteria.getReferrerVersionRefBookId()))
            where.and(isReferrerVersionRefBookId(criteria.getReferrerVersionRefBookId()));

        if (nonNull(criteria.getPublishedVersionId()))
            where.and(isPublishedVersionId(criteria.getPublishedVersionId()));

        if (nonNull(criteria.getPublishedVersionRefBookId()))
            where.and(isPublishedVersionRefBookId(criteria.getPublishedVersionRefBookId()));

        if (nonNull(criteria.getExcludedPublishedVersionId()))
            where.andNot(isPublishedVersionId(criteria.getExcludedPublishedVersionId()));

        return where.getValue();
    }
}

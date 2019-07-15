package ru.inovus.ms.rdm.predicate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.model.conflict.RefBookConflictCriteria;

import static java.util.Objects.nonNull;
import static ru.inovus.ms.rdm.predicate.RefBookConflictPredicates.*;
import static ru.inovus.ms.rdm.predicate.RefBookConflictPredicates.isConflictType;

public class RefBookConflictPredicateProducer {

    private RefBookConflictPredicateProducer() {
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

        if (!CollectionUtils.isEmpty(criteria.getRefRecordIds()))
            where.and(isRefRecordIdIn(criteria.getRefRecordIds()));

        if (nonNull(criteria.getRefFieldCode()))
            where.and(isRefFieldCode(criteria.getRefFieldCode()));

        if (nonNull(criteria.getConflictType()))
            where.and(isConflictType(criteria.getConflictType()));

        return where.getValue();
    }
}

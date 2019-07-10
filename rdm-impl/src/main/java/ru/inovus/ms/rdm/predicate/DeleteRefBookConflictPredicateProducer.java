package ru.inovus.ms.rdm.predicate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import ru.inovus.ms.rdm.model.AbstractCriteria;
import ru.inovus.ms.rdm.model.conflict.DeleteRefBookConflictCriteria;

import static java.util.Objects.nonNull;
import static ru.inovus.ms.rdm.predicate.RefBookConflictPredicates.*;

public class DeleteRefBookConflictPredicateProducer implements CriteriaPredicateProducer {

    private DeleteRefBookConflictPredicateProducer() {
    }

    /**
     * Формирование предиката на основе критерия удаления.
     *
     * @param criteria критерий удаления
     * @return Предикат для удаления
     */
    public static Predicate toPredicate(AbstractCriteria criteria) {

        BooleanBuilder where = new BooleanBuilder();
        if (!(criteria instanceof DeleteRefBookConflictCriteria))
            return where;

        DeleteRefBookConflictCriteria usedCriteria = (DeleteRefBookConflictCriteria) criteria;

        if (nonNull(usedCriteria.getReferrerVersionId()))
            where.and(isReferrerVersionId(usedCriteria.getReferrerVersionId()));

        if (nonNull(usedCriteria.getReferrerVersionRefBookId()))
            where.and(isReferrerVersionRefBookId(usedCriteria.getReferrerVersionRefBookId()));

        if (nonNull(usedCriteria.getPublishedVersionId()))
            where.and(isPublishedVersionId(usedCriteria.getPublishedVersionId()));

        if (nonNull(usedCriteria.getPublishedVersionRefBookId()))
            where.and(isPublishedVersionRefBookId(usedCriteria.getPublishedVersionRefBookId()));

        if (nonNull(usedCriteria.getExcludedPublishedVersionId()))
            where.andNot(isPublishedVersionId(usedCriteria.getExcludedPublishedVersionId()));

        return where.getValue();
    }
}

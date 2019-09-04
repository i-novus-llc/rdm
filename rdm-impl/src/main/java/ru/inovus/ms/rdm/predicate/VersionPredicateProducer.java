package ru.inovus.ms.rdm.predicate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import ru.inovus.ms.rdm.model.version.VersionCriteria;

import static java.util.Objects.nonNull;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.*;

public class VersionPredicateProducer {

    private VersionPredicateProducer() {
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    public static Predicate toPredicate(VersionCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(isVersionOfRefBook(criteria.getRefBookId()));

        if (criteria.getExcludeDraft())
            where.andNot(isDraft());

        if (nonNull(criteria.getVersion()))
            where.and(isVersionNumberContains(criteria.getVersion()));

        return where.getValue();
    }
}

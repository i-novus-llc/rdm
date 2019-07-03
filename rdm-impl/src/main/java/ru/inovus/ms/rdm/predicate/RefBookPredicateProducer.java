package ru.inovus.ms.rdm.predicate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.inovus.ms.rdm.model.refbook.RefBookCriteria;

import static java.util.Objects.nonNull;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.*;

@Component
public class RefBookPredicateProducer implements CriteriaPredicateProducer {

    private PassportPredicateProducer passportPredicateProducer;

    @Autowired
    public RefBookPredicateProducer(PassportPredicateProducer passportPredicateProducer) {
        this.passportPredicateProducer = passportPredicateProducer;
    }

    /**
     * Формирование предиката на основе критерия поиска.
     *
     * @param criteria критерий поиска
     * @return Предикат для запроса поиска
     */
    public Predicate toPredicate(RefBookCriteria criteria) {
        BooleanBuilder where = new BooleanBuilder();

        where.and(isSourceType(criteria.getSourceType()));

        if (nonNull(criteria.getFromDateBegin()))
            where.and(isMaxFromDateEqOrAfter(criteria.getFromDateBegin()));

        if (nonNull(criteria.getFromDateEnd()))
            where.and(isMaxFromDateEqOrBefore(criteria.getFromDateEnd()));

        if (!isEmpty(criteria.getCode()))
            where.and(isCodeContains(criteria.getCode()));

        if (!CollectionUtils.isEmpty(criteria.getPassport()))
            where.and(passportPredicateProducer.toPredicate(criteria.getPassport()));

        if (!isEmpty(criteria.getCategory()))
            where.and(refBookHasCategory(criteria.getCategory()));

        if (!CollectionUtils.isEmpty(criteria.getRefBookIds()))
            where.and(isVersionOfRefBooks(criteria.getRefBookIds()));

        if (criteria.getIsArchived())
            where.and(isArchived());

        else if (criteria.getIsNotArchived())
            where.andNot(isArchived());

        if (criteria.getHasPublished())
            where.andNot(isArchived()).and(isAnyPublished());

        if (criteria.getHasDraft())
            where.andNot(isArchived()).and(refBookHasDraft());

        if (criteria.getHasPublishedVersion())
            where.andNot(isArchived()).and(hasLastPublishedVersion());

        if (criteria.getHasPrimaryAttribute())
            where.and(hasStructure()).and(hasPrimaryAttribute());

        return where.getValue();
    }
}

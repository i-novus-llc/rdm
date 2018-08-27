package ru.inovus.ms.rdm.util;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.model.PassportAttributeValue;

import java.util.Map;

import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.hasAttributeValue;


@Component
public class PassportPredicateProducerImpl implements PassportPredicateProducer {

    @Override
    public Predicate toPredicate(Map<String, PassportAttributeValue> passportAttributeValues) {
        BooleanBuilder where = new BooleanBuilder();
        passportAttributeValues.forEach((k, v) -> where.and(hasAttributeValue(k, v != null ? v.getValue() : null)));
        return where.getValue();
    }



}
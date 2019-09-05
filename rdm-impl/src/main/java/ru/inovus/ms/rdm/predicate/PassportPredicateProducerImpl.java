package ru.inovus.ms.rdm.predicate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.hasPassportAttributeValue;

@Component
@SuppressWarnings("unused")
public class PassportPredicateProducerImpl implements PassportPredicateProducer {

    @Override
    public Predicate toPredicate(Map<String, String> passport) {
        BooleanBuilder where = new BooleanBuilder();
        passport.forEach((k, v) -> where.and(hasPassportAttributeValue(k, v)));
        return where.getValue();
    }
}
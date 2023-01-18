package ru.i_novus.ms.rdm.impl.predicate;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.stereotype.Component;

import java.util.Map;

import static ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates.hasPassportAttributeValue;

@Component
@SuppressWarnings("unused")
public class PassportPredicateProducerImpl implements PassportPredicateProducer {

    @Override
    public Predicate toPredicate(Map<String, String> passport) {

        BooleanBuilder where = new BooleanBuilder();

        passport.forEach((k, v) ->
                where.and(hasPassportAttributeValue(k, v))
        );

        return where.getValue();
    }
}
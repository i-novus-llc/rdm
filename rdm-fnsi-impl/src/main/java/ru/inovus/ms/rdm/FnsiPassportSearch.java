package ru.inovus.ms.rdm;

import com.google.common.collect.ImmutableMap;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.Expressions;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.predicate.PassportPredicateProducer;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static ru.inovus.ms.rdm.predicate.RefBookVersionPredicates.hasPassportAttributeValue;

@Component
@Primary
@SuppressWarnings("unused")
public class FnsiPassportSearch implements PassportPredicateProducer {

    @Override
    public Predicate toPredicate(Map<String, String> passportAttributeValueMap) {
        if (passportAttributeValueMap == null)
            return Expressions.TRUE;

        Map<String, List<String>> orAttributes = ImmutableMap.of(
                "name", Arrays.asList("fullName", "shortName"),
                "OID", Arrays.asList("OID.name", "OID2.name")
        );

        BooleanBuilder where = new BooleanBuilder();
        passportAttributeValueMap.forEach((k, v) -> {
            if (orAttributes.containsKey(k)) {
                where.and(orAttributes.get(k).stream()
                        .map(orAttr -> hasPassportAttributeValue(orAttr, v))
                        .reduce(BooleanExpression::or).get());
            } else
                where.and(hasPassportAttributeValue(k, v));
        });
        return where.getValue();
    }

}
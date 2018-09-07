package ru.inovus.ms.rdm;

import com.google.common.collect.ImmutableMap;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.util.PassportPredicateProducer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.hasAttributeValue;

@Component
@Primary
public class FnsiPassportSearch implements PassportPredicateProducer {

    @Override
    public Predicate toPredicate(Map<String, String> passportAttributeValueMap) {
        Map<String, List<String>> orAttributes = ImmutableMap.of(
                "name", Arrays.asList("fullName", "shortName"),
                "OID", Arrays.asList("OID.name", "OID2.name")
        );
        Map<String, String> tempMap = new HashMap<>(passportAttributeValueMap);

        BooleanBuilder where = new BooleanBuilder();
        tempMap.forEach((k, v) -> {
            if (orAttributes.containsKey(k)){
                where.and(orAttributes.get(k).stream()
                        .map(orAttr -> hasAttributeValue(orAttr, v))
                        .reduce(BooleanExpression::or).get());
            } else
            where.and(hasAttributeValue(k, v));
        });
        return where.getValue();
    }

}
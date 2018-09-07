package ru.inovus.ms.rdm;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.util.PassportPredicateProducer;

import java.util.HashMap;
import java.util.Map;

import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.hasAttributeValue;

@Component
@Primary
public class RdmPassportSearch implements PassportPredicateProducer {

    @Override
    public Predicate toPredicate(Map<String, String> passportAttributeValueMap) {
        Map<String, String> tempMap = new HashMap<>(passportAttributeValueMap);
        String name = tempMap.remove("name");
        String OID = tempMap.remove("OID");
        BooleanBuilder where = new BooleanBuilder();
        tempMap.forEach((k, v) -> where.and(hasAttributeValue(k, v)));
        if (name != null) where.and(hasAttributeValue("fullName", name).or(hasAttributeValue("shortName", name)));
        if (OID != null) where.and(hasAttributeValue("OID", OID).or(hasAttributeValue("OID2", OID)));
        return where.getValue();
    }

}
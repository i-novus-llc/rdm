package ru.inovus.ms.rdm;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Predicate;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.model.PassportAttributeValue;
import ru.inovus.ms.rdm.util.PassportPredicateProducer;

import java.util.HashMap;
import java.util.Map;

import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.hasAttributeValue;

@Component
@Primary
public class RdmPassportSearch implements PassportPredicateProducer {

    @Override
    public Predicate toPredicate(Map<String, PassportAttributeValue> passportAttributeValueMap) {
        Map<String, PassportAttributeValue> tempMap = new HashMap<>(passportAttributeValueMap);
        PassportAttributeValue name = tempMap.remove("name");
        PassportAttributeValue OID = tempMap.remove("OID");
        BooleanBuilder where = new BooleanBuilder();
        tempMap.forEach((k, v) -> where.and(hasAttributeValue(k, v != null ? v.getValue() : null)));
        if (name != null) where.and(hasAttributeValue("fullName", name.getValue()).or(hasAttributeValue("shortName", name.getValue())));
        if (OID != null) where.and(hasAttributeValue("OID1", OID.getValue()).or(hasAttributeValue("OID2", OID.getValue())));
        return where.getValue();
    }

}
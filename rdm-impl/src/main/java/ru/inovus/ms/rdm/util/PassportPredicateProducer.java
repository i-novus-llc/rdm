package ru.inovus.ms.rdm.util;

import com.querydsl.core.types.Predicate;
import ru.inovus.ms.rdm.model.PassportAttributeValue;

import java.util.Map;

/**
 * Created by znurgaliev on 27.08.2018.
 */
public interface PassportPredicateProducer {

    Predicate toPredicate(Map<String, PassportAttributeValue> passportAttributeValueMap);
}

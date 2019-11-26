package ru.inovus.ms.rdm.impl.predicate;

import com.querydsl.core.types.Predicate;

import java.util.Map;

/**
 * Created by znurgaliev on 27.08.2018.
 */
public interface PassportPredicateProducer {

    Predicate toPredicate(Map<String, String> passportAttributeValueMap);
}

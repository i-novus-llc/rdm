package ru.i_novus.ms.rdm.n2o.criteria.construct;

import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;

import java.util.HashMap;
import java.util.Map;

@Component
@Order(1)
public class RefBookCriteriaConstructResolver extends RestCriteriaConstructResolver {

    private static final Map<String, String> FIELD_PROPERTY_MAP = createFieldPropertyMap();

    private static Map<String, String> createFieldPropertyMap() {

        Map<String, String> map = new HashMap<>(2);
        map.put("name", "passport.name");
        map.put("category.id", "category");

        return map;
    }

    @Override
    public boolean isSatisfied(Object instance) {
        return (instance instanceof RefBookCriteria);
    }

    @Override
    protected String toSortProperty(String field) {

        String result = FIELD_PROPERTY_MAP.get(field);
        return (result != null) ? result : super.toSortProperty(field);
    }
}

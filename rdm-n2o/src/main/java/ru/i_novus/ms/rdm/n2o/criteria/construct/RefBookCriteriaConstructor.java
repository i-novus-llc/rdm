package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.framework.api.data.CriteriaConstructor;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookCriteria;

import java.util.HashMap;
import java.util.Map;

@Component
public class RefBookCriteriaConstructor extends RestCriteriaConstructor<RefBookCriteria> {

    private static final Map<String, String> FIELD_PROPERTY_MAP = createFieldPropertyMap();

    private static Map<String, String> createFieldPropertyMap() {

        final Map<String, String> map = new HashMap<>(2);
        map.put("name", "passport.name");
        map.put("category.id", "category");

        return map;
    }

    @Override
    public Class<RefBookCriteria> getCriteriaClass() {
        return RefBookCriteria.class;
    }

    @Override
    public RefBookCriteria construct(N2oPreparedCriteria criteria, RefBookCriteria instance) {
        return prepareInstance(criteria, instance);
    }

    @Override
    protected String toSortProperty(String field) {

        String result = FIELD_PROPERTY_MAP.get(field);
        return (result != null) ? result : super.toSortProperty(field);
    }
}

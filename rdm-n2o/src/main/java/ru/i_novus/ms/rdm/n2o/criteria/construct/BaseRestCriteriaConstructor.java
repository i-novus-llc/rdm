package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import net.n2oapp.platform.jaxrs.RestCriteria;
import org.springframework.stereotype.Component;

/**
 * Базовый класс-резолвер для RestCriteria.
 */
@Component
public class BaseRestCriteriaConstructor extends RestCriteriaConstructor<RestCriteria> {

    @Override
    public Class<RestCriteria> getCriteriaClass() {
        return RestCriteria.class;
    }

    @Override
    public RestCriteria construct(N2oPreparedCriteria criteria, RestCriteria instance) {
        return prepareInstance(criteria, instance);
    }
}

package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.criteria.api.Criteria;
import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Базовый класс-резолвер для Criteria.
 */
@Component
@Order()
public class N2oCriteriaConstructResolver implements CriteriaConstructResolver {

    @Override
    public boolean isSatisfied(Object instance) {
        return (instance instanceof Criteria);
    }

    @Override
    public void resolve(Object instance, N2oPreparedCriteria criteria) {

        Criteria result = ((Criteria) instance);

        result.setSorting(criteria.getSorting());
        result.setPage(criteria.getPage());
        result.setSize(criteria.getSize());
    }
}

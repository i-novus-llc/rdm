package ru.i_novus.ms.rdm.n2o.criteria.construct;

import net.n2oapp.framework.api.criteria.N2oPreparedCriteria;

public interface CriteriaConstructResolver {

    boolean isSatisfied(Object instance);

    void resolve(Object instance, N2oPreparedCriteria criteria);
}

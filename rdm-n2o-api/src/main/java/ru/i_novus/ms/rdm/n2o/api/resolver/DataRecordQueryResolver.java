package ru.i_novus.ms.rdm.n2o.api.resolver;

import net.n2oapp.framework.api.metadata.global.dao.query.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.query.field.QuerySimpleField;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

/**
 * Резолвер для формирования запроса на получение данных.
 */
public interface DataRecordQueryResolver {

    boolean isSatisfied(String dataAction);

    List<QuerySimpleField> createRegularFields(DataRecordRequest request);

    List<N2oQuery.Filter> createRegularFilters(DataRecordRequest request);
}

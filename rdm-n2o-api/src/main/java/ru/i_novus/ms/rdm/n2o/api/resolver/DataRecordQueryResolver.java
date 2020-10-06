package ru.i_novus.ms.rdm.n2o.api.resolver;

import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

/**
 * Резолвер для формирования запроса на получение данных.
 */
public interface DataRecordQueryResolver {

    boolean isSatisfied(String dataAction);

    List<N2oQuery.Field> createRegularFields(DataRecordRequest request);
}

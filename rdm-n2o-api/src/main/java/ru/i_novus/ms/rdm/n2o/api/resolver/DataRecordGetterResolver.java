package ru.i_novus.ms.rdm.n2o.api.resolver;

import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;

import java.io.Serializable;
import java.util.Map;

/**
 * Резолвер для формирования части страницы по отображению данных.
 */
public interface DataRecordGetterResolver {

    boolean isSatisfied(String dataAction);

    Map<String, Serializable> createRegularValues(DataRecordCriteria criteria, RefBookVersion version);

    Map<String, Serializable> createDynamicValues(DataRecordCriteria criteria, RefBookVersion version);
}

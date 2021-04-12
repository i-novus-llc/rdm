package ru.i_novus.ms.rdm.n2o.api.resolver;

import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

/**
 * Резолвер для формирования объекта по выполнению операции.
 */
public interface DataRecordObjectResolver {

    boolean isSatisfied(String dataAction);

    N2oObject.Operation createOperation(DataRecordRequest request);

    List<AbstractParameter> createRegularParams(DataRecordRequest request);

    int getRecordMappingIndex(DataRecordRequest request);
}

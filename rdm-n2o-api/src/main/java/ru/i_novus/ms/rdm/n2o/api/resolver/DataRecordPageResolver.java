package ru.i_novus.ms.rdm.n2o.api.resolver;

import net.n2oapp.framework.api.metadata.SourceComponent;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

/**
 * Резолвер для формирования части страницы по отображению данных.
 */
public interface DataRecordPageResolver {

    boolean isSatisfied(String dataAction);

    List<SourceComponent> createRegularFields(DataRecordRequest request);

    void processDynamicFields(DataRecordRequest request, List<SourceComponent> list);
}

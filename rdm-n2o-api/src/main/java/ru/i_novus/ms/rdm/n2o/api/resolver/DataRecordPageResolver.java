package ru.i_novus.ms.rdm.n2o.api.resolver;

import net.n2oapp.framework.api.metadata.control.N2oField;
import ru.i_novus.ms.rdm.api.model.Structure;

import java.util.List;

/**
 * Резолвер для формирования части страницы по отображению данных.
 */
public interface DataRecordPageResolver {

    boolean isSatisfied(String dataAction);

    List<N2oField> createRegularFields(Integer versionId, Structure structure, String dataAction);

    void processDynamicFields(Integer versionId, Structure structure, String dataAction, List<N2oField> list);
}

package ru.i_novus.ms.rdm.n2o.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordGetterResolver;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_SYSTEM_ID;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class UpdateRecordGetterResolver implements DataRecordGetterResolver {

    @Autowired
    private VersionRestService versionService;

    @Override
    public boolean isSatisfied(String dataAction) {
        return DataRecordConstants.DATA_ACTION_UPDATE.equals(dataAction);
    }

    @Override
    public Map<String, Serializable> createRegularValues(DataRecordCriteria criteria, RefBookVersion version) {

        Map<String, Serializable> map = new HashMap<>(1);

        map.put(FIELD_SYSTEM_ID, criteria.getId());

        return map;
    }

    @Override
    public Map<String, Serializable> createDynamicValues(DataRecordCriteria criteria, RefBookVersion version) {

        List<RefBookRowValue> rowValues = findRowValues(version.getId(), criteria.getId());
        if (isEmpty(rowValues))
            return emptyMap();

        List<FieldValue> fieldValues = rowValues.get(0).getFieldValues();
        Map<String, Serializable> map = new HashMap<>(fieldValues.size());
        fieldValues.forEach(fieldValue ->
                map.put(addPrefix(fieldValue.getField()), fieldValue.getValue())
        );

        return map;
    }

    /**
     * Получение записи из указанной версии справочника по системному идентификатору.
     */
    private List<RefBookRowValue> findRowValues(Integer versionId, Long id) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setRowSystemIds(singletonList(id));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return !isEmpty(rowValues.getContent()) ? rowValues.getContent() : emptyList();
    }
}

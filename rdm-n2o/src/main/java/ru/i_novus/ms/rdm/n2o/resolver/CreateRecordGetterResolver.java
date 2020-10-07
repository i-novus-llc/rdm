package ru.i_novus.ms.rdm.n2o.resolver;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordGetterResolver;
import ru.i_novus.platform.datastorage.temporal.model.Reference;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

@Component
public class CreateRecordGetterResolver implements DataRecordGetterResolver {

    @Override
    public boolean isSatisfied(String dataAction) {
        return DataRecordConstants.DATA_ACTION_CREATE.equals(dataAction);
    }

    @Override
    public Map<String, Serializable> createRegularValues(DataRecordCriteria criteria, RefBookVersion version) {
        return emptyMap();
    }

    @Override
    public Map<String, Serializable> createDynamicValues(DataRecordCriteria criteria, RefBookVersion version) {

        Structure structure = version.getStructure();

        // Значения по умолчанию при создании записи заполнять здесь.
        Map<String, Serializable> map = new HashMap<>(structure.getAttributes().size());

        // Get default values from backend by versionService.searchDefaults(versionId) instead of:
        version.getStructure().getReferences().forEach(reference ->
                map.put(addPrefix(reference.getAttribute()), new Reference())
        );

        return map;
    }
}

package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.model.refdata.UpdateDataRequest;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordGetterResolver;

import java.io.Serializable;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.RowUtils.prepareRowValues;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;

@Controller
public class DataRecordController {

    @Autowired
    private VersionRestService versionService;

    @Autowired
    private DraftRestService draftService;

    @Autowired
    private Collection<DataRecordGetterResolver> resolvers;

    /**
     * Получение записи версии справочника по параметрам.
     *
     * @param criteria критерий поиска
     */
    @SuppressWarnings("unused") // used in: *RecordQueryResolver
    public Map<String, Serializable> getRow(DataRecordCriteria criteria) {

        String dataAction = criteria.getDataAction();
        if (StringUtils.isEmpty(dataAction))
            throw new IllegalArgumentException("A data action is not specified");

        RefBookVersion version = versionService.getById(criteria.getVersionId());

        Map<String, Serializable> map = createDefaultValues(version, criteria);

        getSatisfiedResolvers(dataAction).forEach(resolver -> {
            map.putAll(resolver.createRegularValues(criteria, version));
            map.putAll(resolver.createDynamicValues(criteria, version));
        });

        return map;
    }

    /** Создание набора для заполнения. */
    private Map<String, Serializable> createDefaultValues(RefBookVersion version, DataRecordCriteria criteria) {

        Map<String, Serializable> map = new HashMap<>(3);

        map.put(FIELD_VERSION_ID, version.getId());
        map.put(FIELD_OPT_LOCK_VALUE, criteria.getOptLockValue());
        map.put(FIELD_DATA_ACTION, criteria.getDataAction());

        return map;
    }

    /**
     * Обновление строки данных версии справочника.
     * @param draftId      идентификатор черновика справочника
     * @param optLockValue значение оптимистической блокировки версии-черновика
     * @param row          строка данных для добавления/изменения
     */
    @SuppressWarnings("WeakerAccess")
    public void updateData(Integer draftId, Integer optLockValue, Row row) {

        prepareRowValues(row);

        UpdateDataRequest request = new UpdateDataRequest(optLockValue, singletonList(row));
        draftService.updateData(draftId, request);
    }

    private Stream<DataRecordGetterResolver> getSatisfiedResolvers(String dataAction) {

        if (isEmpty(resolvers))
            return Stream.empty();

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(dataAction));
    }
}

package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.l10n.VersionLocaleService;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordGetterResolver;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_LOCALE_CODE;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_SYSTEM_ID;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.DATA_ACTION_LOCALIZE;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.FIELD_LOCALE_NAME;

@Component
@SuppressWarnings({"rawtypes", "java:S3740"})
public class L10nLocalizeRecordGetterResolver implements DataRecordGetterResolver {

    private final VersionRestService versionService;

    private final VersionLocaleService versionLocaleService;

    @Autowired
    public L10nLocalizeRecordGetterResolver(VersionRestService versionService,
                                            VersionLocaleService versionLocaleService) {
        this.versionService = versionService;
        this.versionLocaleService = versionLocaleService;
    }

    @Override
    public boolean isSatisfied(String dataAction) {
        return DATA_ACTION_LOCALIZE.equals(dataAction);
    }

    @Override
    public Map<String, Serializable> createRegularValues(DataRecordCriteria criteria, RefBookVersion version) {

        final Map<String, Serializable> map = new HashMap<>(3);

        map.put(FIELD_SYSTEM_ID, criteria.getId());

        final String localeCode = criteria.getLocaleCode();
        map.put(FIELD_LOCALE_CODE, localeCode);
        map.put(FIELD_LOCALE_NAME, getLocaleName(localeCode));

        return map;
    }

    /** Получение наименования локали по коду. */
    private String getLocaleName(String localeCode) {

        if (StringUtils.isEmpty(localeCode))
            throw new IllegalArgumentException("Locale code is empty");

        return versionLocaleService.getLocaleName(localeCode);
    }

    @Override
    public Map<String, Serializable> createDynamicValues(DataRecordCriteria criteria, RefBookVersion version) {

        final List<RefBookRowValue> rowValues = findRowValues(criteria);
        if (isEmpty(rowValues))
            return emptyMap();

        final List<FieldValue> fieldValues = rowValues.get(0).getFieldValues();
        final Map<String, Serializable> map = new HashMap<>(fieldValues.size());

        fieldValues.forEach(fieldValue ->
                map.put(addPrefix(fieldValue.getField()), fieldValue.getValue())
        );
        return map;
    }

    /**
     * Получение записи из указанной версии справочника по системному идентификатору.
     */
    private List<RefBookRowValue> findRowValues(DataRecordCriteria criteria) {

        final SearchDataCriteria dataCriteria = new SearchDataCriteria();
        dataCriteria.setLocaleCode(criteria.getLocaleCode());
        dataCriteria.setRowSystemIds(singletonList(criteria.getId()));

        final Page<RefBookRowValue> rowValues = searchRowValues(criteria.getVersionId(), dataCriteria);
        return !isEmpty(rowValues.getContent()) ? rowValues.getContent() : emptyList();
    }

    private Page<RefBookRowValue> searchRowValues(Integer versionId, SearchDataCriteria dataCriteria) {

        final Page<RefBookRowValue> rowValues = versionService.search(versionId, dataCriteria);
        if (!isEmpty(rowValues.getContent()))
            return rowValues;

        dataCriteria.setLocaleCode(null);
        return versionService.search(versionId, dataCriteria);
    }
}

package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordGetterResolver;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;

import java.io.Serializable;
import java.nio.charset.StandardCharsets;
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
@SuppressWarnings("java:S3740")
public class L10nLocalizeRecordGetterResolver implements DataRecordGetterResolver {

    @SuppressWarnings("java:S1075")
    private static final String GET_LOCALE_NAME_FORMAT = "/l10n/locale/name/%s";

    @Autowired
    private VersionRestService versionService;

    @Value("${rdm.backend.path}")
    private String restUrl;

    @Override
    public boolean isSatisfied(String dataAction) {
        return DATA_ACTION_LOCALIZE.equals(dataAction);
    }

    @Override
    public Map<String, Serializable> createRegularValues(DataRecordCriteria criteria, RefBookVersion version) {

        Map<String, Serializable> map = new HashMap<>(2);

        map.put(FIELD_SYSTEM_ID, criteria.getId());

        String localeCode = criteria.getLocaleCode();
        map.put(FIELD_LOCALE_CODE, localeCode);
        map.put(FIELD_LOCALE_NAME, getLocaleName(localeCode));

        return map;
    }

    /** Получение наименования локали по коду. */
    private String getLocaleName(String localeCode) {

        if (StringUtils.isEmpty(localeCode))
            throw new IllegalArgumentException("Locale code is empty");

        try {
            RestTemplate restTemplate = createRestTemplate();

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(restUrl)
                    .path(String.format(GET_LOCALE_NAME_FORMAT, localeCode));
            String localeName = restTemplate.getForObject(builder.toUriString(), String.class);

            return StringUtils.isEmpty(localeName) ? localeCode : localeName;

        } catch (RuntimeException e) {
            return localeCode;
        }
    }

    private RestTemplate createRestTemplate() {

        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        return restTemplate;
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
    private List<RefBookRowValue> findRowValues(Integer versionId, Integer id) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        criteria.setRowSystemIds(singletonList(id.longValue()));

        Page<RefBookRowValue> rowValues = versionService.search(versionId, criteria);
        return !isEmpty(rowValues.getContent()) ? rowValues.getContent() : emptyList();
    }
}

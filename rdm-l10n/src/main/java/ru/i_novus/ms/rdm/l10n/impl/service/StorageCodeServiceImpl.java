package ru.i_novus.ms.rdm.l10n.impl.service;

import ru.i_novus.ms.rdm.l10n.api.model.criteria.StorageCodeCriteria;
import ru.i_novus.ms.rdm.l10n.api.service.StorageCodeService;
import ru.i_novus.platform.datastorage.temporal.util.CollectionUtils;
import ru.i_novus.platform.l10n.versioned_data_storage.model.L10nConstants;
import ru.i_novus.platform.versioned_data_storage.pg_impl.dao.StorageConstants;
import ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StringUtils.isNullOrEmpty;

public class StorageCodeServiceImpl implements StorageCodeService {

    private static final String WRONG_SCHEMA_CHAR_REGEX = "[^A-Za-z0-9_]";
    private static final String SCHEMA_CHAR_REPLACEMENT = "0";

    @Override
    public String toStorageCode(StorageCodeCriteria criteria) {

        String storageCode = criteria.getStorageCode();
        String schemaName = getSchemaName(criteria);
        return StorageUtils.toStorageCode(schemaName, storageCode);
    }

    @Override
    @SuppressWarnings("java:S2259")
    public String getSchemaName(StorageCodeCriteria criteria) {

        return toSchemaName(criteria.getLocaleCode());
    }


    @Override
    public String toSchemaName(String localeCode) {

        if (isNullOrEmpty(localeCode))
            return null;

        if (L10nConstants.DEFAULT_LOCALE_CODE.equals(localeCode))
            return StorageConstants.DATA_SCHEMA_NAME;

        localeCode = localeCode.replaceAll(WRONG_SCHEMA_CHAR_REGEX, SCHEMA_CHAR_REPLACEMENT);
        return L10nConstants.SCHEMA_NAME_PREFIX + localeCode.toLowerCase();
    }

    public Map<String, String> toLocaleSchemas(List<String> localeCodes) {

        if (CollectionUtils.isNullOrEmpty(localeCodes))
            return emptyMap();

        return localeCodes.stream().collect(toMap(identity(), this::toSchemaName));
    }
}

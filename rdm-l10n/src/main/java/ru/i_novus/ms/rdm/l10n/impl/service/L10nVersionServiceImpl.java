package ru.i_novus.ms.rdm.l10n.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.service.L10nVersionService;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.isDefaultSchema;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.isValidSchemaName;

@SuppressWarnings("java:S3740")
public class L10nVersionServiceImpl implements L10nVersionService {

    private static final String LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE = "locale.code.not.found";
    private static final String LOCALE_CODE_IS_INVALID_EXCEPTION_CODE = "locale.code.is.invalid";
    private static final String STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE = "storage.code.not.found";

    private L10nDraftDataService draftDataService;
    private L10nStorageCodeService storageCodeService;

    private VersionService versionService;

    @Autowired
    public L10nVersionServiceImpl(L10nDraftDataService draftDataService,
                                  L10nStorageCodeService storageCodeService,
                                  VersionService versionService) {

        this.draftDataService = draftDataService;
        this.storageCodeService = storageCodeService;

        this.versionService = versionService;
    }

    @Override
    public String localizeTable(Integer versionId, String localeCode) {

        if (isEmpty(localeCode))
            throw new IllegalArgumentException(LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE);

        String sourceTableName = versionService.getStorageCode(versionId);
        if (isEmpty(sourceTableName))
            throw new IllegalArgumentException(STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE);

        String targetSchemaName = toSchemaName(localeCode);
        String targetCode = draftDataService.createLocalizedTable(sourceTableName, targetSchemaName);

        // Копирование всех колонок записей, FTS обновляется по триггеру.
        draftDataService.copyAllData(sourceTableName, targetCode);

        return targetCode;
    }

    @Override
    public void localizeData(Integer versionId, LocalizeDataRequest request) {

        String localeCode = request.getLocaleCode();
        localizeTable(versionId, localeCode);

        Structure structure = versionService.getStructure(versionId);
        List<RowValue> updatedRowValues = request.getRows().stream()
                .map(row -> ConverterUtil.rowValue(row, structure))
                .filter(rowValue -> rowValue.getSystemId() != null)
                .collect(toList());
        if (CollectionUtils.isEmpty(updatedRowValues))
            return;

        String schemaName = toSchemaName(localeCode);
        draftDataService.updateRows(schemaName, updatedRowValues);
    }

    private String toSchemaName(String localeCode) {

        String schemaName = storageCodeService.toLocaleSchema(localeCode);
        if (isDefaultSchema(schemaName) || !isValidSchemaName(schemaName))
            throw new UserException(new Message(LOCALE_CODE_IS_INVALID_EXCEPTION_CODE, localeCode));

        return schemaName;
    }
}

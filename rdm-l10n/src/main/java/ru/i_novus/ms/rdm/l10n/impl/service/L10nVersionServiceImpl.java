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
import static ru.i_novus.platform.datastorage.temporal.util.StorageUtils.isDefaultSchema;
import static ru.i_novus.platform.datastorage.temporal.util.StorageUtils.isValidSchemaName;

public class L10nVersionServiceImpl implements L10nVersionService {

    private static final String LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE = "locale.code.not.found";
    private static final String LOCALE_CODE_IS_INVALID_EXCEPTION_CODE = "locale.code.is.invalid";
    private static final String STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE = "storage.code.not.found";

    private L10nStorageCodeService storageCodeService;
    private L10nDraftDataService draftDataService;

    private VersionService versionService;

    @Autowired
    public L10nVersionServiceImpl(L10nStorageCodeService storageCodeService,
                                  L10nDraftDataService draftDataService,
                                  VersionService versionService) {

        this.storageCodeService = storageCodeService;
        this.draftDataService = draftDataService;

        this.versionService = versionService;
    }

    @Override
    public void localizeTable(Integer versionId, String localeCode) {

        if (isEmpty(localeCode))
            throw new IllegalArgumentException(LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE);

        String schemaName = toSchemaName(localeCode);
        if (!draftDataService.schemaExists(schemaName)) {
            draftDataService.createSchema(schemaName);
        }

        String sourceCode = versionService.getStorageCode(versionId);
        if (isEmpty(sourceCode))
            throw new IllegalArgumentException(STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE);

        draftDataService.createLocalizedTable(sourceCode, schemaName);
    }

    @Override
    public void localizeData(Integer versionId, LocalizeDataRequest request) {

        String localeCode = request.getLocaleCode();
        localizeTable(versionId, localeCode);

        // Замена записей на локализованные.
        String schemaName = toSchemaName(localeCode);

        Structure structure = versionService.getStructure(versionId);
        List<RowValue> updatedRowValues = request.getRows().stream()
                .map(row -> ConverterUtil.rowValue(row, structure))
                .filter(rowValue -> rowValue.getSystemId() != null)
                .collect(toList());
        if (CollectionUtils.isEmpty(updatedRowValues))
            return;

        draftDataService.updateRows(schemaName, updatedRowValues);
    }

    private String toSchemaName(String localeCode) {

        String schemaName = storageCodeService.toSchemaName(localeCode);
        if (isDefaultSchema(schemaName) || !isValidSchemaName(schemaName))
            throw new UserException(new Message(LOCALE_CODE_IS_INVALID_EXCEPTION_CODE, localeCode));

        return schemaName;
    }
}

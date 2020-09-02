package ru.i_novus.ms.rdm.l10n.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.service.L10nVersionStorageService;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.isDefaultSchema;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.isValidSchemaName;

@Service
@SuppressWarnings("java:S3740")
public class L10nVersionStorageServiceImpl implements L10nVersionStorageService {

    private static final String LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE = "locale.code.not.found";
    private static final String LOCALE_CODE_IS_DEFAULT_EXCEPTION_CODE = "locale.code.is.default";
    private static final String LOCALE_CODE_IS_INVALID_EXCEPTION_CODE = "locale.code.is.invalid";
    private static final String STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE = "storage.code.not.found";

    private L10nDraftDataService draftDataService;
    private L10nStorageCodeService storageCodeService;

    private RefBookVersionRepository versionRepository;

    @Autowired
    public L10nVersionStorageServiceImpl(L10nDraftDataService draftDataService,
                                         L10nStorageCodeService storageCodeService,
                                         RefBookVersionRepository versionRepository) {

        this.draftDataService = draftDataService;
        this.storageCodeService = storageCodeService;

        this.versionRepository = versionRepository;
    }

    @Override
    public String localizeTable(Integer versionId, String localeCode) {

        if (isEmpty(localeCode))
            throw new IllegalArgumentException(LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE);

        RefBookVersionEntity versionEntity = getVersionOrThrow(versionId);
        String sourceTableName = versionEntity.getStorageCode();
        if (isEmpty(sourceTableName))
            throw new IllegalArgumentException(STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE);

        String targetSchemaName = toValidSchemaName(localeCode);
        String targetCode = draftDataService.createLocalizedTable(sourceTableName, targetSchemaName);

        // Копирование всех колонок записей, FTS обновляется по триггеру.
        draftDataService.copyAllData(sourceTableName, targetCode);

        return targetCode;
    }

    @Override
    public void localizeData(Integer versionId, LocalizeDataRequest request) {

        String localeCode = request.getLocaleCode();
        localizeTable(versionId, localeCode);

        RefBookVersionEntity versionEntity = getVersionOrThrow(versionId);
        Structure structure = versionEntity.getStructure();
        List<RowValue> updatedRowValues = request.getRows().stream()
                .map(row -> ConverterUtil.rowValue(row, structure))
                .filter(rowValue -> rowValue.getSystemId() != null)
                .collect(toList());
        if (CollectionUtils.isEmpty(updatedRowValues))
            return;

        String schemaName = toValidSchemaName(localeCode);
        draftDataService.updateRows(schemaName, updatedRowValues);
    }

    @Override
    public String getLocaleStorageCode(String storageCode, String localeCode) {

        return storageCodeService.toStorageCode(storageCode, localeCode);
    }

    private String toValidSchemaName(String localeCode) {

        String schemaName = storageCodeService.toSchemaName(localeCode);
        if (isDefaultSchema(schemaName))
            throw new UserException(new Message(LOCALE_CODE_IS_DEFAULT_EXCEPTION_CODE, localeCode));

        if (!isValidSchemaName(schemaName))
            throw new UserException(new Message(LOCALE_CODE_IS_INVALID_EXCEPTION_CODE, localeCode));

        return schemaName;
    }

    private RefBookVersionEntity getVersionOrThrow(Integer versionId) {
        return versionRepository.findById(versionId)
                .orElseThrow(() -> new NotFoundException(new Message(VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE, versionId)));
    }
}

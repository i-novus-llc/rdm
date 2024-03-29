package ru.i_novus.ms.rdm.l10n.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.DraftChangeRequest;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.service.l10n.L10nService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeTableRequest;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.util.StringUtils;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;

import java.util.List;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.*;

@Primary
@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class L10nServiceImpl implements L10nService {

    private static final String LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE = "locale.code.not.found";
    private static final String LOCALE_CODE_IS_DEFAULT_EXCEPTION_CODE = "locale.code.is.default";
    private static final String LOCALE_CODE_IS_INVALID_EXCEPTION_CODE = "locale.code.is.invalid";
    private static final String STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE = "storage.code.not.found";

    private final L10nDraftDataService draftDataService;
    private final L10nStorageCodeService storageCodeService;

    private final RefBookVersionRepository versionRepository;
    private final VersionValidation versionValidation;

    @Autowired
    public L10nServiceImpl(L10nDraftDataService draftDataService,
                           L10nStorageCodeService storageCodeService,
                           RefBookVersionRepository versionRepository,
                           VersionValidation versionValidation) {

        this.draftDataService = draftDataService;
        this.storageCodeService = storageCodeService;

        this.versionRepository = versionRepository;
        this.versionValidation = versionValidation;
    }

    @Override
    public void localizeData(Integer versionId, LocalizeDataRequest request) {

        if (StringUtils.isNullOrEmpty(request.getLocaleCode()))
            throw new IllegalArgumentException(LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE);

        if (CollectionUtils.isNullOrEmpty(request.getRows()))
            return;

        RefBookVersionEntity versionEntity = getVersionOrThrow(versionId);
        validateOptLockValue(versionEntity, request);

        String targetCode = localizeTable(versionEntity, request);

        Structure structure = toLocalizableStructure(versionEntity.getStructure());
        List<RowValue> updatedRowValues = toRowValues(request.getRows(), structure);
        if (CollectionUtils.isNullOrEmpty(updatedRowValues))
            return;

        draftDataService.localizeRows(targetCode, updatedRowValues);
    }

    /** Создание копии таблицы версии для локализации записей. */
    private String localizeTable(RefBookVersionEntity versionEntity, LocalizeTableRequest request) {

        String sourceTableName = versionEntity.getStorageCode();
        if (StringUtils.isNullOrEmpty(sourceTableName))
            throw new IllegalArgumentException(STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE);

        String targetSchemaName = toValidSchemaName(request.getLocaleCode());
        String targetCode = toStorageCode(targetSchemaName, sourceTableName);

        if (!draftDataService.storageExists(targetCode)) {

            targetCode = draftDataService.createLocalizedTable(sourceTableName, targetSchemaName);

            // Копирование всех колонок записей, FTS обновляется по триггеру.
            draftDataService.copyAllData(sourceTableName, targetCode);
        }

        return targetCode;
    }

    /** Получение структуры для перевода на основе структуры версии. */
    private Structure toLocalizableStructure(Structure structure) {

        List<Structure.Attribute> attributes = structure.getLocalizables();
        if (isEmpty(attributes))
            return new Structure();

        if (attributes.size() == structure.getAttributes().size())
            return structure;

        List<Structure.Reference> references = structure.getReferences();
        if (references.isEmpty())
            return new Structure(attributes, null);

        references = attributes.stream()
                .map(attribute -> structure.getReference(attribute.getCode()))
                .filter(Objects::nonNull)
                .collect(toList());

        return new Structure(attributes, references);
    }

    private List<RowValue> toRowValues(List<Row> rows, Structure structure) {

        return rows.stream()
                .map(row -> ConverterUtil.rowValue(row, structure))
                .filter(rowValue -> rowValue.getSystemId() != null)
                .collect(toList());
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

    private void validateOptLockValue(RefBookVersionEntity entity, DraftChangeRequest request) {

        versionValidation.validateOptLockValue(entity.getId(), entity.getOptLockValue(), request.getOptLockValue());
    }
}

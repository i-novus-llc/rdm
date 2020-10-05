package ru.i_novus.ms.rdm.l10n.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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
import ru.i_novus.ms.rdm.l10n.api.model.L10nVersionLocale;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeTableRequest;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nLocaleInfoService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;
import ru.i_novus.platform.l10n.versioned_data_storage.model.L10nLocaleInfo;
import ru.i_novus.platform.l10n.versioned_data_storage.model.criteria.L10nLocaleCriteria;
import ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.*;

@Primary
@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class L10nServiceImpl implements L10nService {

    private static final String LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE = "locale.code.not.found";
    private static final String LOCALE_CODE_IS_DEFAULT_EXCEPTION_CODE = "locale.code.is.default";
    private static final String LOCALE_CODE_IS_INVALID_EXCEPTION_CODE = "locale.code.is.invalid";
    private static final String STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE = "storage.code.not.found";

    private L10nDraftDataService draftDataService;
    private L10nLocaleInfoService localeInfoService;
    private L10nStorageCodeService storageCodeService;

    private RefBookVersionRepository versionRepository;
    private VersionValidation versionValidation;

    @Autowired
    public L10nServiceImpl(L10nDraftDataService draftDataService,
                           L10nLocaleInfoService localeInfoService,
                           L10nStorageCodeService storageCodeService,
                           RefBookVersionRepository versionRepository,
                           VersionValidation versionValidation) {

        this.draftDataService = draftDataService;
        this.localeInfoService = localeInfoService;
        this.storageCodeService = storageCodeService;

        this.versionRepository = versionRepository;
        this.versionValidation = versionValidation;
    }

    @Override
    public String localizeTable(Integer versionId, LocalizeTableRequest request) {

        if (isEmpty(request.getLocaleCode()))
            throw new IllegalArgumentException(LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE);

        RefBookVersionEntity versionEntity = getVersionOrThrow(versionId);
        validateOptLockValue(versionEntity, request);

        return localizeTable(versionEntity, request);
    }

    @Override
    public void localizeData(Integer versionId, LocalizeDataRequest request) {

        if (isEmpty(request.getLocaleCode()))
            throw new IllegalArgumentException(LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE);

        if (CollectionUtils.isEmpty(request.getRows()))
            return;

        RefBookVersionEntity versionEntity = getVersionOrThrow(versionId);
        validateOptLockValue(versionEntity, request);

        String targetCode = localizeTable(versionEntity, request);

        Structure structure = toLocalizableStructure(versionEntity.getStructure());
        List<RowValue> updatedRowValues = toRowValues(request.getRows(), structure);
        if (CollectionUtils.isEmpty(updatedRowValues))
            return;

        draftDataService.localizeRows(targetCode, updatedRowValues);
    }

    /** Получение структуры для перевода на основе структуры версии. */
    private Structure toLocalizableStructure(Structure structure) {

        List<Structure.Attribute> attributes = structure.getAttributes().stream()
                    .filter(Structure.Attribute::isLocalizable)
                    .collect(toList());
        if (attributes.size() == structure.getAttributes().size())
            return structure;

        List<Structure.Reference> references = structure.getReferences();
        if (references.isEmpty())
            return new Structure(attributes, null);

        List<String> attributeCodes = attributes.stream().map(Structure.Attribute::getCode).collect(toList());
        references = structure.getReferences().stream()
                .filter(reference -> attributeCodes.contains(reference.getAttribute()))
                .collect(toList());

        return new Structure(attributes, references);
    }

    private List<RowValue> toRowValues(List<Row> rows, Structure structure) {

        return rows.stream()
                .map(row -> ConverterUtil.rowValue(row, structure))
                .filter(rowValue -> rowValue.getSystemId() != null)
                .collect(toList());
    }

    /**
     * Создание копии таблицы версии для локализации записей.
     */
    private String localizeTable(RefBookVersionEntity versionEntity, LocalizeTableRequest request) {

        String sourceTableName = versionEntity.getStorageCode();
        if (isEmpty(sourceTableName))
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

    @Override
    public Page<L10nVersionLocale> searchVersionLocales(Integer versionId) {

        L10nLocaleCriteria criteria = new L10nLocaleCriteria();
        criteria.makeUnpaged();
        List<L10nLocaleInfo> localeInfos = localeInfoService.search(criteria);
        List<String> localeCodes = localeInfos.stream().map(L10nLocaleInfo::getCode).collect(toList());

        Map<String, String> localeSchemas = storageCodeService.toSchemaNames(localeCodes);

        List<L10nVersionLocale> list = localeSchemas.entrySet().stream()
                .filter(e -> !StorageUtils.isDefaultSchema(e.getValue()))
                .map(e -> toVersionLocale(versionId, findLocaleInfo(e.getKey(), localeInfos)))
                .filter(Objects::nonNull)
                .collect(toList());

        return new PageImpl<>(list, Pageable.unpaged(), list.size());
    }

    @Override
    public L10nVersionLocale getVersionLocale(Integer versionId, String localeCode) {

        L10nLocaleInfo localeInfo = localeInfoService.find(localeCode);

        String localeSchemaName = storageCodeService.toSchemaName(localeCode);
        if (isDefaultSchema(localeSchemaName))
            throw new UserException(new Message(LOCALE_CODE_IS_DEFAULT_EXCEPTION_CODE, localeCode));

        return toVersionLocale(versionId, localeInfo);
    }

    @Override
    public String getLocaleName(String localeCode) {

        L10nLocaleInfo localeInfo = localeInfoService.find(localeCode);
        return localeInfo != null ? localeInfo.getName() : null;
    }

    private L10nLocaleInfo findLocaleInfo(String localeCode, List<L10nLocaleInfo> localeInfos) {

        return localeInfos.stream()
                .filter(info -> localeCode.equals(info.getCode()))
                .findFirst().orElse(null);
    }

    private L10nVersionLocale toVersionLocale(Integer versionId, L10nLocaleInfo info) {

        if (info == null)
            return null;

        L10nVersionLocale model = new L10nVersionLocale();
        model.setVersionId(versionId);
        model.setLocaleCode(info.getCode());
        model.setLocaleName(info.getName());
        model.setLocaleSelfName(info.getSelfName());

        return model;
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

    private void validateOptLockValue(RefBookVersionEntity entity, DraftChangeRequest request) {

        versionValidation.validateOptLockValue(entity.getId(), entity.getOptLockValue(), request.getOptLockValue());
    }
}

package ru.i_novus.ms.rdm.l10n.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.service.AuditLogService;
import ru.i_novus.ms.rdm.impl.service.DraftServiceImpl;
import ru.i_novus.ms.rdm.impl.service.RefBookLockService;
import ru.i_novus.ms.rdm.impl.validation.StructureChangeValidator;
import ru.i_novus.ms.rdm.l10n.api.service.L10nVersionStorageService;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.model.L10nConstants;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.BooleanField;

import java.util.List;

@Primary
@Service
@SuppressWarnings("java:S3740")
public class L10nDraftServiceImpl extends DraftServiceImpl {

    private L10nVersionStorageService versionStorageService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public L10nDraftServiceImpl(L10nVersionStorageService versionStorageService,
                                RefBookVersionRepository versionRepository, RefBookConflictRepository conflictRepository,
                                DraftDataService draftDataService, DropDataService dropDataService,
                                SearchDataService searchDataService,
                                RefBookLockService refBookLockService, VersionService versionService,
                                FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                                VersionFileService versionFileService,
                                VersionValidation versionValidation,
                                PassportValueRepository passportValueRepository,
                                AttributeValidationRepository attributeValidationRepository,
                                StructureChangeValidator structureChangeValidator,
                                AuditLogService auditLogService) {

        super(versionRepository, conflictRepository,
                draftDataService, dropDataService, searchDataService,
                refBookLockService, versionService,
                fileStorage, fileNameGenerator, versionFileService,
                versionValidation,
                passportValueRepository, attributeValidationRepository, structureChangeValidator,
                auditLogService);

        this.versionStorageService = versionStorageService;
    }

    @Override
    protected List<Field> makeOutputFields(RefBookVersionEntity version, String localeCode) {

        List<Field> fields = super.makeOutputFields(version, localeCode);

        if (!StringUtils.isEmpty(localeCode)) {
            fields.add(new BooleanField(L10nConstants.SYS_LOCALIZED));
        }

        return fields;
    }

    @Override
    protected String toLocaleStorageCode(String storageCode, String localeCode) {

        return versionStorageService.getLocaleStorageCode(storageCode, localeCode);
    }
}

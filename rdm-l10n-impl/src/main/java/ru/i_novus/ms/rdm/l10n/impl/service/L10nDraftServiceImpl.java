package ru.i_novus.ms.rdm.l10n.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.*;
import ru.i_novus.ms.rdm.impl.service.AuditLogService;
import ru.i_novus.ms.rdm.impl.service.DraftServiceImpl;
import ru.i_novus.ms.rdm.impl.service.RefBookLockService;
import ru.i_novus.ms.rdm.impl.strategy.StrategyLocator;
import ru.i_novus.ms.rdm.l10n.api.model.L10nConstants;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.DropDataService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.BooleanField;

import java.util.List;

@Primary
@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class L10nDraftServiceImpl extends DraftServiceImpl {

    private final L10nStorageCodeService storageCodeService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public L10nDraftServiceImpl(L10nStorageCodeService storageCodeService,
                                RefBookVersionRepository versionRepository,
                                RefBookConflictRepository conflictRepository,
                                DraftDataService draftDataService, DropDataService dropDataService,
                                SearchDataService searchDataService,
                                RefBookLockService refBookLockService, VersionService versionService,
                                VersionValidation versionValidation,
                                PassportValueRepository passportValueRepository,
                                AttributeValidationRepository attributeValidationRepository,
                                VersionFileService versionFileService,
                                AuditLogService auditLogService,
                                StrategyLocator strategyLocator) {

        super(versionRepository, conflictRepository,
                draftDataService, dropDataService, searchDataService,
                refBookLockService, versionService,
                versionValidation,
                passportValueRepository, attributeValidationRepository,
                versionFileService,
                auditLogService,
                strategyLocator);

        this.storageCodeService = storageCodeService;
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
    protected String toStorageCode(RefBookVersionEntity draft, SearchDataCriteria criteria) {

        return storageCodeService.toStorageCode(draft.getStorageCode(), criteria.getLocaleCode());
    }
}

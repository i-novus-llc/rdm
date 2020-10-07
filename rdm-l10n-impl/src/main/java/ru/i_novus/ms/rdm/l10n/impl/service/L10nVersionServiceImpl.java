package ru.i_novus.ms.rdm.l10n.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.service.AuditLogService;
import ru.i_novus.ms.rdm.impl.service.VersionServiceImpl;
import ru.i_novus.ms.rdm.l10n.api.model.L10nConstants;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nStorageCodeService;
import ru.i_novus.platform.versioned_data_storage.pg_impl.model.BooleanField;

import java.util.List;

@Primary
@Service
@SuppressWarnings({"rawtypes", "java:S3740"})
public class L10nVersionServiceImpl extends VersionServiceImpl {

    private L10nStorageCodeService storageCodeService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public L10nVersionServiceImpl(L10nStorageCodeService storageCodeService,
                                  RefBookVersionRepository versionRepository,
                                  SearchDataService searchDataService,
                                  FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                                  VersionFileRepository versionFileRepository, VersionFileService versionFileService,
                                  AuditLogService auditLogService) {

        super(versionRepository, searchDataService,
                fileStorage, fileNameGenerator,
                versionFileRepository, versionFileService,
                auditLogService);

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
    protected String toStorageCode(RefBookVersionEntity version, SearchDataCriteria criteria) {

        return storageCodeService.toStorageCode(version.getStorageCode(), criteria.getLocaleCode());
    }
}

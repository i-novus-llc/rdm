package ru.i_novus.ms.rdm.l10n.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.service.VersionFileService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;
import ru.i_novus.ms.rdm.impl.file.FileStorage;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.repository.VersionFileRepository;
import ru.i_novus.ms.rdm.impl.service.AuditLogService;
import ru.i_novus.ms.rdm.impl.service.VersionServiceImpl;
import ru.i_novus.ms.rdm.l10n.api.service.L10nVersionStorageService;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

@SuppressWarnings("java:S3740")
@Primary
@Service
@Qualifier("VersionServiceImpl")
public class L10nVersionServiceImpl extends VersionServiceImpl implements VersionService {

    private L10nVersionStorageService versionStorageService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public L10nVersionServiceImpl(L10nVersionStorageService versionStorageService,
                                  RefBookVersionRepository versionRepository,
                                  SearchDataService searchDataService,
                                  FileStorage fileStorage, FileNameGenerator fileNameGenerator,
                                  VersionFileRepository versionFileRepository, VersionFileService versionFileService,
                                  AuditLogService auditLogService) {

        super(versionRepository, searchDataService,
                fileStorage, fileNameGenerator,
                versionFileRepository, versionFileService,
                auditLogService);

        this.versionStorageService = versionStorageService;
    }
}

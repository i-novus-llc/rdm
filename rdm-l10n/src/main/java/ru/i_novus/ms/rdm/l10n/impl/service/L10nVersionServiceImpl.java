package ru.i_novus.ms.rdm.l10n.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.l10n.api.service.L10nVersionService;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.l10n.versioned_data_storage.pg_impl.dao.L10nDataDao;
import ru.i_novus.platform.l10n.versioned_data_storage.pg_impl.service.L10nStorageCodeService;

import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.platform.datastorage.temporal.util.StorageUtils.*;

public class L10nVersionServiceImpl implements L10nVersionService {

    private static final String LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE = "locale.code.not.found";
    private static final String LOCALE_CODE_IS_INVALID_EXCEPTION_CODE = "locale.code.is.invalid";
    private static final String STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE = "storage.code.not.found";

    private L10nDataDao dataDao;
    private L10nStorageCodeService storageCodeService;
    private DraftDataService draftDataService;

    private VersionService versionService;

    @Autowired
    public L10nVersionServiceImpl(L10nDataDao dataDao,
                                  L10nStorageCodeService storageCodeService, DraftDataService draftDataService,
                                  VersionService versionService) {

        this.dataDao = dataDao;
        this.storageCodeService = storageCodeService;
        this.draftDataService = draftDataService;

        this.versionService = versionService;
    }

    @Override
    public void localizeData(Integer versionId, LocalizeDataRequest request) {

        String localeCode = request.getLocaleCode();
        if (isEmpty(localeCode))
            throw new IllegalArgumentException(LOCALE_CODE_NOT_FOUND_EXCEPTION_CODE);

        String schemaName = storageCodeService.toSchemaName(localeCode);
        if (isDefaultSchema(schemaName) || !isValidSchemaName(schemaName))
            throw new UserException(new Message(LOCALE_CODE_IS_INVALID_EXCEPTION_CODE, localeCode));

        if (!draftDataService.schemaExists(schemaName)) {
            draftDataService.createSchema(schemaName);
        }

        String sourceCode = versionService.getStorageCode(versionId);
        if (isEmpty(sourceCode))
            throw new IllegalArgumentException(STORAGE_CODE_NOT_FOUND_EXCEPTION_CODE);

        String targetCode = toStorageCode(schemaName, sourceCode);
        if (!draftDataService.storageExists(sourceCode)) {
            dataDao.createLocalizedTable(sourceCode, targetCode);
        }

        // Замена записей на локализованные.
    }
}

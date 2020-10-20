package ru.i_novus.ms.rdm.l10n.impl.service;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.service.l10n.VersionLocaleService;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;
import ru.i_novus.ms.rdm.l10n.api.model.L10nVersionLocale;
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
import static ru.i_novus.platform.versioned_data_storage.pg_impl.util.StorageUtils.isDefaultSchema;

@Primary
@Service
@SuppressWarnings("java:S3740")
public class VersionLocaleServiceImpl implements VersionLocaleService {

    private static final String LOCALE_CODE_IS_DEFAULT_EXCEPTION_CODE = "locale.code.is.default";

    private final L10nLocaleInfoService localeInfoService;
    private final L10nStorageCodeService storageCodeService;
    private final L10nDraftDataService draftDataService;

    private final RefBookVersionRepository versionRepository;

    @Autowired
    public VersionLocaleServiceImpl(L10nLocaleInfoService localeInfoService,
                                    L10nStorageCodeService storageCodeService,
                                    L10nDraftDataService draftDataService,
                                    RefBookVersionRepository versionRepository) {

        this.localeInfoService = localeInfoService;
        this.storageCodeService = storageCodeService;
        this.draftDataService = draftDataService;

        this.versionRepository = versionRepository;
    }

    public List<String> findRefBookLocales(String refBookCode) {

        RefBookVersionEntity versionEntity = versionRepository.findFirstByRefBookCodeAndStatusOrderByFromDateDesc(refBookCode, RefBookVersionStatus.PUBLISHED);
        if (versionEntity == null)
            throw new NotFoundException(new Message(VersionValidationImpl.LAST_PUBLISHED_NOT_FOUND_EXCEPTION_CODE, refBookCode));

        return draftDataService.findTableLocaleCodes(versionEntity.getStorageCode());
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
}

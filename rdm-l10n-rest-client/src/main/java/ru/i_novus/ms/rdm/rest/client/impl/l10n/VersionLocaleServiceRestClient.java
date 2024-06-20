package ru.i_novus.ms.rdm.rest.client.impl.l10n;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.service.l10n.VersionLocaleService;
import ru.i_novus.ms.rdm.l10n.api.model.L10nVersionLocale;
import ru.i_novus.ms.rdm.rest.client.feign.l10n.VersionLocaleServiceFeignClient;

import java.util.List;

public class VersionLocaleServiceRestClient implements VersionLocaleService {

    private final VersionLocaleServiceFeignClient client;

    public VersionLocaleServiceRestClient(VersionLocaleServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public List<String> findRefBookLocales(String refBookCode) {
        return client.findRefBookLocales(refBookCode);
    }

    @Override
    public Page<L10nVersionLocale> searchVersionLocales(Integer versionId) {
        return client.searchVersionLocales(versionId);
    }

    @Override
    public L10nVersionLocale getVersionLocale(Integer versionId, String localeCode) {
        return client.getVersionLocale(versionId, localeCode);
    }

    @Override
    public String getLocaleName(String localeCode) {
        return client.getLocaleName(localeCode);
    }
}

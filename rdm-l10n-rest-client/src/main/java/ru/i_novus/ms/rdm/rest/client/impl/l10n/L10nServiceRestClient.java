package ru.i_novus.ms.rdm.rest.client.impl.l10n;

import ru.i_novus.ms.rdm.api.service.l10n.L10nService;
import ru.i_novus.ms.rdm.l10n.api.model.LocalizeDataRequest;
import ru.i_novus.ms.rdm.rest.client.feign.l10n.L10nServiceFeignClient;

public class L10nServiceRestClient implements L10nService {

    private final L10nServiceFeignClient client;

    public L10nServiceRestClient(L10nServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public void localizeData(Integer versionId, LocalizeDataRequest request) {
        client.localizeData(versionId, request);
    }
}

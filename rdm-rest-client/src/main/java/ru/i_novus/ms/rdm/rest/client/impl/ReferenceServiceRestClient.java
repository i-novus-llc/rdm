package ru.i_novus.ms.rdm.rest.client.impl;

import ru.i_novus.ms.rdm.api.service.ReferenceService;
import ru.i_novus.ms.rdm.rest.client.feign.ReferenceServiceFeignClient;

public class ReferenceServiceRestClient implements ReferenceService {

    private final ReferenceServiceFeignClient client;

    public ReferenceServiceRestClient(ReferenceServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public void refreshReferrer(Integer referrerVersionId, Integer optLockValue) {
        client.refreshReferrer(referrerVersionId, optLockValue);
    }

    @Override
    public void refreshLastReferrers(String refBookCode) {
        client.refreshLastReferrers(refBookCode);
    }
}

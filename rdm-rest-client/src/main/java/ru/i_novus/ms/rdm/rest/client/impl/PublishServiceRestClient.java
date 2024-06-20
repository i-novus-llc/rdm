package ru.i_novus.ms.rdm.rest.client.impl;

import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.rest.client.feign.PublishServiceFeignClient;

import java.util.UUID;

public class PublishServiceRestClient implements PublishService {

    private final PublishServiceFeignClient client;

    public PublishServiceRestClient(PublishServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public void publish(Integer draftId, PublishRequest request) {
        client.publish(draftId, request);
    }

    @Override
    public UUID publishAsync(Integer draftId, PublishRequest request) {
        return client.publishAsync(draftId, request);
    }
}

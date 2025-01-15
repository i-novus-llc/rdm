package ru.i_novus.ms.rdm.impl.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.rest.PublishRestService;
import ru.i_novus.ms.rdm.api.service.PublishService;

@Primary
@Service
public class PublishRestServiceImpl implements PublishRestService {

    private final PublishService syncPublishService;

    private final PublishService asyncPublishService;

    @Autowired
    public PublishRestServiceImpl(
            @Qualifier("syncPublishService") PublishService syncPublishService,
            @Qualifier("asyncPublishService") PublishService asyncPublishService
    ) {
        this.syncPublishService = syncPublishService;
        this.asyncPublishService = asyncPublishService;
    }

    @Override
    public void publish(Integer draftId, PublishRequest request) {

        syncPublishService.publish(draftId, request);
    }

    @Override
    public void publishAsync(Integer draftId, PublishRequest request) {

        asyncPublishService.publish(draftId, request);
    }
}

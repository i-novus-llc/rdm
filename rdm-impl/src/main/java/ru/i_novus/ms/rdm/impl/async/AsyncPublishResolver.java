package ru.i_novus.ms.rdm.impl.async;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationResolver;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;

import java.io.Serializable;

@Component
public class AsyncPublishResolver implements AsyncOperationResolver {

    private static final String PUBLSH_REQUEST_IS_UNKNOWN = "Request for publication of draft %s is unknown: %s";

    private final PublishService publishService;

    public AsyncPublishResolver(PublishService publishService) {
        this.publishService = publishService;
    }

    @Override
    public boolean isSatisfied(AsyncOperationTypeEnum operationType) {

        return AsyncOperationTypeEnum.PUBLICATION.equals(operationType);
    }

    @Override
    public Serializable resolve(String code, Serializable[] args) {

        Integer draftId = (Integer) args[0];

        Object request = args[1];
        if (request instanceof PublishRequest) {
            publishService.publish(draftId, (PublishRequest) request);

            return null;
        }

        throw new IllegalArgumentException(String.format(PUBLSH_REQUEST_IS_UNKNOWN, draftId, request));
    }
}

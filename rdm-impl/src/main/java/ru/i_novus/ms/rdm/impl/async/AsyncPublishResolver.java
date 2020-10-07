package ru.i_novus.ms.rdm.impl.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationResolver;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;

import java.io.Serializable;

@Component
public class AsyncPublishResolver implements AsyncOperationResolver {

    private static final String PUBLSH_REQUEST_IS_UNKNOWN = "Request for publication of '%s' is unknown (draft = %s, request: %s)";

    private final PublishService publishService;

    @Autowired
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

        Serializable argRequest = args[1];
        if (argRequest instanceof PublishRequest) {
            publishService.publish(draftId, (PublishRequest) argRequest);

            return null;
        }

        throw new IllegalArgumentException(String.format(PUBLSH_REQUEST_IS_UNKNOWN, code, draftId, argRequest));
    }
}

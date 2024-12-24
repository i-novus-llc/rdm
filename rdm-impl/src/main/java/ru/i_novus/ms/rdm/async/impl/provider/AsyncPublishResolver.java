package ru.i_novus.ms.rdm.async.impl.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.model.draft.PublishRequest;
import ru.i_novus.ms.rdm.api.service.PublishService;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationResolver;

import java.io.Serializable;

@Component
public class AsyncPublishResolver implements AsyncOperationResolver {

    static final String NAME = "AsyncPublish";

    private static final String PUBLISH_REQUEST_IS_UNKNOWN = "Request for publication of '%s' is unknown (draft = %s, request: %s)";

    private PublishService publishService;

    public AsyncPublishResolver() {
        // Nothing to do.
    }

    @Autowired
    public void setPublishService(@Lazy PublishService publishService) {
        this.publishService = publishService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isSatisfied(AsyncOperationTypeEnum operationType) {

        return AsyncOperationTypeEnum.PUBLICATION.equals(operationType);
    }

    @Override
    public Serializable resolve(AsyncOperationMessage message) {

        if (message == null) return null;

        final AsyncOperationTypeEnum operationType = message.getOperationType();
        if (!isSatisfied(operationType)) return null;

        final Serializable[] args = message.getArgs();
        final Integer draftId = (Integer) args[0];
        final Serializable argRequest = args[1];

        if (argRequest instanceof PublishRequest publishRequest) {

            publishService.publish(draftId, publishRequest);

            return null;
        }

        final String errorMessage = String.format(PUBLISH_REQUEST_IS_UNKNOWN, message.getCode(), draftId, argRequest);
        throw new IllegalArgumentException(errorMessage);
    }
}

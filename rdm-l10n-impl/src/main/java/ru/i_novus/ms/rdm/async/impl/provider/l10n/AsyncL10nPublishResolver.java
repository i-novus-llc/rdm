package ru.i_novus.ms.rdm.async.impl.provider.l10n;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.async.api.model.AsyncOperationMessage;
import ru.i_novus.ms.rdm.async.api.provider.AsyncOperationResolver;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;

import java.io.Serializable;

@Component
public class AsyncL10nPublishResolver implements AsyncOperationResolver {

    static final String NAME = "AsyncL10nPublish";

    private static final String L10N_PUBLISH_REQUEST_IS_UNKNOWN = "Request for publication of localization '%s' is unknown (request: %s)";

    private L10nDraftDataService draftDataService;

    public AsyncL10nPublishResolver() {
        // Nothing to do.
    }

    @Autowired
    public void setDraftDataService(@Lazy L10nDraftDataService draftDataService) {
        this.draftDataService = draftDataService;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean isSatisfied(AsyncOperationTypeEnum operationType) {

        return AsyncOperationTypeEnum.L10N_PUBLICATION.equals(operationType);
    }

    @Override
    public Serializable resolve(AsyncOperationMessage message) {

        if (message == null) return null;

        final AsyncOperationTypeEnum operationType = message.getOperationType();
        if (!isSatisfied(operationType)) return null;

        final Serializable[] args = message.getArgs();
        final Serializable argRequest = args[0];

        if (argRequest instanceof PostPublishRequest request) {

            draftDataService.applyLocalizedDraft(request.getLastStorageCode(),
                    request.getOldStorageCode(), request.getNewStorageCode(),
                    request.getFromDate(), request.getToDate());
            return null;
        }

        final String errorMessage = String.format(L10N_PUBLISH_REQUEST_IS_UNKNOWN, message.getCode(), argRequest);
        throw new IllegalArgumentException(errorMessage);
    }
}

package ru.i_novus.ms.rdm.l10n.impl.async;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.async.AsyncOperationResolver;
import ru.i_novus.ms.rdm.api.async.AsyncOperationTypeEnum;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;

import java.io.Serializable;

@Component
public class AsyncL10nPublishResolver implements AsyncOperationResolver {

    private static final String L10N_PUBLSH_REQUEST_IS_UNKNOWN = "Request for publication of localization '%s' is unknown (request: %s)";

    private final L10nDraftDataService draftDataService;

    @Autowired
    public AsyncL10nPublishResolver(L10nDraftDataService draftDataService) {
        this.draftDataService = draftDataService;
    }

    @Override
    public boolean isSatisfied(AsyncOperationTypeEnum operationType) {

        return AsyncOperationTypeEnum.L10N_PUBLICATION.equals(operationType);
    }

    @Override
    public Serializable resolve(String code, Serializable[] args) {

        Serializable argRequest = args[0];
        if (argRequest instanceof PostPublishRequest) {
            PostPublishRequest request = (PostPublishRequest) argRequest;

            draftDataService.applyLocalizedDraft(request.getLastStorageCode(),
                    request.getOldStorageCode(), request.getNewStorageCode(),
                    request.getFromDate(), request.getToDate());
            return null;
        }

        throw new IllegalArgumentException(String.format(L10N_PUBLSH_REQUEST_IS_UNKNOWN, code, argRequest));
    }
}

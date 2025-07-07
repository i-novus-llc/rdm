package ru.i_novus.ms.rdm.l10n.impl.provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.draft.PostPublishRequest;
import ru.i_novus.ms.rdm.impl.provider.PostPublishResolver;
import ru.i_novus.platform.l10n.versioned_data_storage.api.service.L10nDraftDataService;

@Component
public class PostPublishL10nPublishResolver implements PostPublishResolver {

    private L10nDraftDataService draftDataService;

    public PostPublishL10nPublishResolver() {
        // Nothing to do.
    }

    @Autowired
    public void setDraftDataService(@Lazy L10nDraftDataService draftDataService) {
        this.draftDataService = draftDataService;
    }

    @Override
    public void resolve(PostPublishRequest request) {

        if (request == null) return;

        draftDataService.applyLocalizedDraft(
                request.getLastStorageCode(),
                request.getOldStorageCode(), request.getNewStorageCode(),
                request.getFromDate(), request.getToDate()
        );
    }
}

package ru.i_novus.ms.rdm.n2o.strategy.draft;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.draft.Draft;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.n2o.api.model.UiDraft;

@Component
public class DefaultFindOrCreateDraftStrategy implements FindOrCreateDraftStrategy {

    @Autowired
    private DraftRestService draftService;

    @Override
    public UiDraft findOrCreate(RefBookVersion version) {

        if (version.isDraft()) {
            return new UiDraft(version);
        }

        final Integer refBookId = version.getRefBookId();

        Draft draft = draftService.findDraft(version.getCode());
        if (draft == null) {
            draft = draftService.createFromVersion(version.getId());
        }

        return new UiDraft(draft, refBookId);
    }
}

package ru.i_novus.ms.rdm.impl.strategy.draft;

import net.n2oapp.platform.i18n.Message;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class DefaultValidateDraftExistsStrategy implements ValidateDraftExistsStrategy {

    private static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";

    @Override
    public void validate(RefBookVersionEntity entity, Integer id) {

        if (entity == null || !entity.isChangeable()) {
            throw new NotFoundException(
                    new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, id)
            );
        }
    }
}

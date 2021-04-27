package ru.i_novus.ms.rdm.impl.strategy.draft;

import net.n2oapp.platform.i18n.Message;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;

@Component
public class DefaultValidateDraftExistsStrategy implements ValidateDraftExistsStrategy {

    @Override
    public void validate(RefBookVersionEntity entity, Integer id) {

        if (!isDraft(entity)) {
            throw new NotFoundException(
                    new Message(VersionValidationImpl.DRAFT_NOT_FOUND_EXCEPTION_CODE, id)
            );
        }
    }

    protected boolean isDraft(RefBookVersionEntity entity) {

        return entity != null &&
                RefBookVersionStatus.DRAFT.equals(entity.getStatus());
    }
}

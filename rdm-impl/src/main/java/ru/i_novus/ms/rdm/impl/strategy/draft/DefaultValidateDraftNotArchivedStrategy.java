package ru.i_novus.ms.rdm.impl.strategy.draft;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;

@Component
public class DefaultValidateDraftNotArchivedStrategy implements ValidateDraftNotArchivedStrategy {

    @Override
    public void validate(RefBookVersionEntity entity) {

        if (Boolean.TRUE.equals(entity.getRefBook().getArchived())) {
            throw new UserException(
                    new Message(VersionValidationImpl.REFBOOK_WITH_CODE_IS_ARCHIVED_EXCEPTION_CODE, entity.getRefBook().getCode())
            );
        }
    }
}

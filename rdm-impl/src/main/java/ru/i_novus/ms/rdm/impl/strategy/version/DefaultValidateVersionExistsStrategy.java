package ru.i_novus.ms.rdm.impl.strategy.version;

import net.n2oapp.platform.i18n.Message;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.validation.VersionValidationImpl;

@Component
public class DefaultValidateVersionExistsStrategy implements ValidateVersionExistsStrategy {

    @Override
    public void validate(RefBookVersionEntity entity) {

        if (!isVersion(entity)) {
            throw new NotFoundException(
                    new Message(VersionValidationImpl.VERSION_NOT_FOUND_EXCEPTION_CODE, getId(entity))
            );
        }
    }

    private Integer getId(RefBookVersionEntity entity) {
        return (entity != null) ? entity.getId() : null;
    }

    protected boolean isVersion(RefBookVersionEntity entity) {

        return entity != null;
    }
}

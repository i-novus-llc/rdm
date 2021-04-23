package ru.i_novus.ms.rdm.n2o.strategy.draft;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

@Component
public class DefaultValidateIsDraftStrategy implements ValidateIsDraftStrategy {

    private static final String VERSION_IS_NOT_DRAFT_EXCEPTION_CODE = "version.is.not.draft";

    @Override
    public void validate(RefBookVersion version) {

        if (!version.isDraft())
            throw new UserException(new Message(VERSION_IS_NOT_DRAFT_EXCEPTION_CODE, version.getId()));
    }
}

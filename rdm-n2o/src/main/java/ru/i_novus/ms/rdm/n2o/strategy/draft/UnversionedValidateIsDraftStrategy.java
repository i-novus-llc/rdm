package ru.i_novus.ms.rdm.n2o.strategy.draft;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

@Component
public class UnversionedValidateIsDraftStrategy implements ValidateIsDraftStrategy {

    @Override
    public void validate(RefBookVersion version) {
        // Nothing to do.
    }
}

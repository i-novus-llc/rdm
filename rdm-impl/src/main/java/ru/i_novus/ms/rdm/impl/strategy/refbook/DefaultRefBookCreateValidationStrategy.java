package ru.i_novus.ms.rdm.impl.strategy.refbook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;

@Component
public class DefaultRefBookCreateValidationStrategy implements RefBookCreateValidationStrategy {

    @Autowired
    private VersionValidation versionValidation;

    @Override
    public void validate(String refBookCode) {

        versionValidation.validateRefBookCode(refBookCode);
        versionValidation.validateRefBookCodeNotExists(refBookCode);
    }
}

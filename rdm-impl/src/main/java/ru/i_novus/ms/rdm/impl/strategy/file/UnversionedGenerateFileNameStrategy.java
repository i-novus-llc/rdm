package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

@Component
public class UnversionedGenerateFileNameStrategy extends DefaultGenerateFileNameStrategy {

    @Override
    protected String getVersionPart(RefBookVersion version) {
        return "";
    }
}

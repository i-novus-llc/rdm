package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

@Component
public class DefaultGenerateFileNameStrategy implements GenerateFileNameStrategy {

    @Override
    public String generateName(RefBookVersion version, FileType fileType) {

        return version.getCode() +
                getVersionPart(version) +
                "." + fileType.name().toLowerCase();
    }

    @Override
    public String generateZipName(RefBookVersion version, FileType fileType) {

        return version.getCode() +
                getVersionPart(version) +
                "_" + fileType.name() + ".zip";
    }

    protected String getVersionPart(RefBookVersion version) {

        return "_" + (version.isDraft() ? "0.0" : version.getVersion());
    }
}

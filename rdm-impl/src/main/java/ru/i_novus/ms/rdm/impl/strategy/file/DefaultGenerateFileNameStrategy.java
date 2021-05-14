package ru.i_novus.ms.rdm.impl.strategy.file;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;

@Component
public class DefaultGenerateFileNameStrategy implements GenerateFileNameStrategy {

    @Override
    public String generateName(RefBookVersion version, FileType fileType) {

        return version.getCode() + "_" +
                (version.isDraft() ? "0.0" : version.getVersion()) +
                "." + fileType.name().toLowerCase();
    }

    @Override
    public String generateZipName(RefBookVersion version, FileType fileType) {

        return version.getCode() + "_" +
                (version.isDraft() ? "0.0" : version.getVersion()) +
                "_" + fileType.name() + ".zip";
    }
}

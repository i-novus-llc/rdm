package ru.i_novus.ms.rdm.impl.file.export;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;

/**
 * Created by znurgaliev on 06.08.2018.
 */
@Component
public class FileNameGeneratorImpl implements FileNameGenerator {

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

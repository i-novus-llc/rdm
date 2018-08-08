package ru.inovus.ms.rdm.file.export;

import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.util.FileNameGenerator;

/**
 * Created by znurgaliev on 06.08.2018.
 */
@Component
public class FileNameGeneratorImpl implements FileNameGenerator {

    @Override
    public String generateName(RefBookVersion version, FileType fileType) {
        return version.getCode() +
                (RefBookVersionStatus.DRAFT.equals(version.getStatus()) ? "_draft" : version.getVersion()) +
                "." + fileType.name().toLowerCase();
    }

    @Override
    public String generateZipName(RefBookVersion version, FileType fileTypes) {
        return version.getCode() +
                (RefBookVersionStatus.DRAFT.equals(version.getStatus()) ? "_draft" : version.getVersion()) +
                ".zip";
    }
}

package ru.inovus.ms.rdm.rest;

import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.util.FileNameGenerator;

/**
 * Created by znurgaliev on 07.08.2018.
 */
public class FnsiFileNameGenerator implements FileNameGenerator {
    @Override
    public String generateName(RefBookVersion version, FileType fileType) {
        return "" + version.getPassport().getAttributes().get("OID") + "_" +
                (RefBookVersionStatus.DRAFT.equals(version.getStatus()) ? "draft" : version.getVersion()) +
                "." + fileType.name().toLowerCase();
    }

    @Override
    public String generateZipName(RefBookVersion version, FileType fileType) {
        return "" + version.getPassport().getAttributes().get("OID") + "_" +
                (RefBookVersionStatus.DRAFT.equals(version.getStatus()) ? "draft" : version.getVersion()) +
                "_" + fileType.name() + ".zip";
    }
}

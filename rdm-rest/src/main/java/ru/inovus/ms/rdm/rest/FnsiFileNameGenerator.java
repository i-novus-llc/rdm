package ru.inovus.ms.rdm.rest;

import ru.inovus.ms.rdm.enumeration.FileType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.model.PassportAttributeValue;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.util.FileNameGenerator;

import java.util.Optional;

/**
 * Created by znurgaliev on 07.08.2018.
 */
public class FnsiFileNameGenerator implements FileNameGenerator {
    @Override
    public String generateName(RefBookVersion version, FileType fileType) {
        String value = Optional.ofNullable(version.getPassport().get("OID")).map(PassportAttributeValue::getValue).orElse("");
        return value + "_" +
                (RefBookVersionStatus.DRAFT.equals(version.getStatus()) ? "0.0" : version.getVersion()) +
                "." + fileType.name().toLowerCase();
    }

    @Override
    public String generateZipName(RefBookVersion version, FileType fileType) {
        String value = Optional.ofNullable(version.getPassport().get("OID")).map(PassportAttributeValue::getValue).orElse("");
        return value + "_" +
                (RefBookVersionStatus.DRAFT.equals(version.getStatus()) ? "draft" : version.getVersion()) +
                "_" + fileType.name() + ".zip";
    }
}

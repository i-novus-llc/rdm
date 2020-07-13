package ru.i_novus.ms.rdm.rest;

import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.util.FileNameGenerator;

import java.util.Optional;

/**
 * Created by znurgaliev on 07.08.2018.
 */
public class FnsiFileNameGenerator implements FileNameGenerator {

    private static final String PREFIX_PASSPORT_ATTRIBUTE = "OID.name";

    @Override
    public String generateName(RefBookVersion version, FileType fileType) {
        String value = Optional.ofNullable(version.getPassport().get(PREFIX_PASSPORT_ATTRIBUTE)).orElse("");
        return value + "_" +
                (version.isDraft() ? "0.0" : version.getVersion()) +
                "." + fileType.name().toLowerCase();
    }

    @Override
    public String generateZipName(RefBookVersion version, FileType fileType) {
        String value = Optional.ofNullable(version.getPassport().get(PREFIX_PASSPORT_ATTRIBUTE)).orElse("");
        return value + "_" +
                (version.isDraft() ? "0.0" : version.getVersion()) +
                "_" + fileType.name() + ".zip";
    }
}

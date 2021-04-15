package ru.i_novus.ms.rdm.impl.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.util.VersionNumberStrategy;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SequenceVersionNumberStrategy implements VersionNumberStrategy {

    private static final String VERSION_REGEX = "\\d*\\.\\d*";
    private static final  Pattern VERSION_PATTERN = Pattern.compile("^(" + VERSION_REGEX + ")");
    private static final String FIRST_VERSION = "1.0";
    private static final String SEQUENCE_START_VERSION = "0.0";

    private final RefBookVersionRepository versionRepository;

    @Autowired
    public SequenceVersionNumberStrategy(RefBookVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Override
    public String first() {
        return FIRST_VERSION;
    }

    @Override
    @Transactional
    public String next(Integer refBookId) {
        List<RefBookVersionEntity> versionEntityList =
                versionRepository.findAllByStatusAndRefBookId(RefBookVersionStatus.PUBLISHED, refBookId);

        RefBookVersionEntity maxVersion = getMaxVersion(versionEntityList);

        if (maxVersion == null) return FIRST_VERSION;
        Matcher matcher = VERSION_PATTERN.matcher(maxVersion.getVersion());
        String s = matcher.find() ? matcher.group() : SEQUENCE_START_VERSION;

        String[] versionParts = s.split("\\.");
        int major = Integer.parseInt(versionParts[0]);
        int minor = Integer.parseInt(versionParts[1]);
        RefBookVersionEntity draft = versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookId);
        if (draft != null && draft.getStructure().equals(maxVersion.getStructure())){
            minor++;
            return major + "." + minor;
        } else {
            return ++major + ".0";
        }
    }

    private RefBookVersionEntity getMaxVersion(List<RefBookVersionEntity> versionEntityList) {

        return versionEntityList.stream().reduce((v1, v2) -> {

            String[] version1Parts = v1.getVersion().split("\\.");
            int major1 = Integer.parseInt(version1Parts[0]);
            String[] version2Parts = v2.getVersion().split("\\.");
            int major2 = Integer.parseInt(version2Parts[0]);

            if (major1 > major2) return v1;
            if (major2 > major1) return v2;

            int minor1 = Integer.parseInt(version1Parts[1]);
            int minor2 = Integer.parseInt(version2Parts[1]);
            if (minor1 > minor2) return v1;
            else return v2;

        }).orElse(null);
    }

    @Override
    public boolean check(String version, Integer refBookId) {

        if (version == null || !version.matches(VERSION_REGEX))
            return false;

        return versionRepository.findAllByStatusAndRefBookId(RefBookVersionStatus.PUBLISHED, refBookId)
                .stream().noneMatch(versionEntity -> versionEntity.getVersion().equals(version));
    }
}

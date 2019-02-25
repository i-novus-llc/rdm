package ru.inovus.ms.rdm.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import javax.transaction.Transactional;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SequenceVersionNumberStrategy implements VersionNumberStrategy {

    private final Pattern pattern = Pattern.compile("^(\\d*\\.\\d*)");


    RefBookVersionRepository versionRepository;

    @Autowired
    public SequenceVersionNumberStrategy(RefBookVersionRepository versionRepository) {
        this.versionRepository = versionRepository;
    }

    @Override
    @Transactional
    public String next(Integer refbookId) {
        List<RefBookVersionEntity> versionEntityList =
                versionRepository.findAllByStatusAndRefBookId(RefBookVersionStatus.PUBLISHED, refbookId);

        RefBookVersionEntity maxVersion = getMaxVersion(versionEntityList);

        if (maxVersion == null) return "1.0";
        Matcher matcher = pattern.matcher(maxVersion.getVersion());
        String s = "0.0";
        if (matcher.find())
            s = matcher.group();
        String[] versionParts = s.split("\\.");
        Integer major = Integer.parseInt(versionParts[0]);
        Integer minor = Integer.parseInt(versionParts[1]);
        RefBookVersionEntity draft = versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refbookId);
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
            Integer major1 = Integer.parseInt(version1Parts[0]);
            String[] version2Parts = v2.getVersion().split("\\.");
            Integer major2 = Integer.parseInt(version2Parts[0]);
            if (major1 > major2) return v1;
            if (major2 > major1) return v2;

            Integer minor1 = Integer.parseInt(version1Parts[1]);
            Integer minor2 = Integer.parseInt(version2Parts[1]);
            if (minor1 > minor2) return v1;
            else return v2;
        }).orElse(null);
    }

    @Override
    public boolean check(String version, Integer refbookId) {
        if (version == null || !version.matches("\\d*\\.\\d*")) return false;
        return versionRepository.findAllByStatusAndRefBookId(RefBookVersionStatus.PUBLISHED, refbookId)
                .stream().noneMatch(versionEntity -> versionEntity.getVersion().equals(version));
    }
}

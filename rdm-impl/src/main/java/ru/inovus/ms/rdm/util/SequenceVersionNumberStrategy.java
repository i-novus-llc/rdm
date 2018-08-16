package ru.inovus.ms.rdm.util;


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
        if (draft != null && draft.getStructure().equals(maxVersion.getStructure()))
            minor++;
        else major++;
        return major + "." + minor;
    }

    private RefBookVersionEntity getMaxVersion(List<RefBookVersionEntity> versionEntityList) {
        return versionEntityList.stream().reduce((v1, v2) -> {

            if (v1 == null || v1.getVersion() == null) return v2;
            if (v2 == null || v2.getVersion() == null) return v1;

            Matcher matcher1 = pattern.matcher(v1.getVersion());
            Matcher matcher2 = pattern.matcher(v2.getVersion());
            Double num1 = Double.parseDouble((matcher1.find() ? matcher1.group() : "-1"));
            Double num2 = Double.parseDouble((matcher2.find() ? matcher2.group() : "-1"));
            if (num1 > num2) return v1;
            else if (num2 != -1) return v2;
            return null;
        }).orElse(null);
    }

    @Override
    public boolean check(String version, Integer refbookId) {
        if (version == null || !version.matches("\\d*\\.\\d*")) return false;
        return versionRepository.findAllByStatusAndRefBookId(RefBookVersionStatus.PUBLISHED, refbookId)
                .stream().noneMatch(versionEntity -> versionEntity.getVersion().equals(version));
    }
}

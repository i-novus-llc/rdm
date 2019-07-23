package ru.inovus.ms.rdm.util;

import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.version.RefBookVersion;

import java.util.LinkedHashMap;
import java.util.Objects;

import static java.util.stream.Collectors.toMap;

/**
 * Created by znurgaliev on 09.08.2018.
 */
public class ModelGenerator {

    private ModelGenerator() {
    }

    public static RefBookVersion versionModel(RefBookVersionEntity entity) {
        if (entity == null) return null;

        RefBookVersion model = new RefBookVersion();
        model.setId(entity.getId());
        model.setRefBookId(entity.getRefBook().getId());
        model.setCode(entity.getRefBook().getCode());
        model.setCategory(entity.getRefBook().getCategory());
        model.setComment(entity.getComment());
        model.setVersion(entity.getVersion());
        model.setFromDate(entity.getFromDate());
        model.setToDate(entity.getToDate());
        model.setStatus(entity.getStatus());
        model.setArchived(entity.getRefBook().getArchived());

        if (entity.getPassportValues() != null)
            model.setPassport(entity.getPassportValues().stream()
                    .filter(v -> Objects.nonNull(v.getValue()))
                    .sorted((o1, o2) -> {
                        if (o1.getAttribute().getPosition() == null || o2.getAttribute().getPosition() == null)
                            return 0;
                        return o1.getAttribute().getPosition() - o2.getAttribute().getPosition();
                    })
                    .collect(toMap(
                            v -> v.getAttribute().getCode(),
                            PassportValueEntity::getValue,
                            (e1, e2) -> e2,
                            LinkedHashMap::new)));

        model.setStructure(entity.getStructure());
        model.setEditDate(entity.getLastActionDate());

        return model;
    }
}

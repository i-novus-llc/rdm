package ru.inovus.ms.rdm.util;

import ru.inovus.ms.rdm.entity.PassportValueEntity;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.RefBookVersion;

import java.util.Objects;
import java.util.stream.Collectors;

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
        model.setComment(entity.getComment());
        model.setVersion(entity.getVersion());
        model.setFromDate(entity.getFromDate());
        model.setToDate(entity.getToDate());
        model.setArchived(entity.getRefBook().getArchived());
        model.setStatus(entity.getStatus());
        model.setEditDate(entity.getLastActionDate());
        if (entity.getPassportValues() != null)
            model.setPassport(entity.getPassportValues().stream()
                    .filter(v -> Objects.nonNull(v.getValue()))
                    .sorted((o1, o2) -> {
                        if (o1.getAttribute().getPosition() == null || o2.getAttribute().getPosition() == null)
                            return 0;
                        return o1.getAttribute().getPosition() - o2.getAttribute().getPosition();
                    })
                    .collect(Collectors.toMap(
                            v -> v.getAttribute().getCode(),
                            PassportValueEntity::getValue)));
        return model;
    }
}

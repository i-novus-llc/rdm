package ru.inovus.ms.rdm.impl.util;

import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;

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
        model.setStatus(entity.getStatus());
        model.setArchived(entity.getRefBook().getArchived());

        if (entity.getPassportValues() != null) {
            model.setPassport(entity.toPassport());
        }

        model.setStructure(entity.getStructure());
        model.setEditDate(entity.getLastActionDate());

        return model;
    }
}

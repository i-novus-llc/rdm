package ru.i_novus.ms.rdm.impl.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

@Entity
@DiscriminatorValue(RefBookTypeEnum.VALUES.DEFAULT)
public class DefaultRefBookEntity extends RefBookEntity {

    public DefaultRefBookEntity() {
        super(RefBookTypeEnum.DEFAULT);
    }

    @Override
    public RefBookVersionEntity createChangeableVersion() {

        RefBookVersionEntity result = new RefBookVersionEntity();
        result.setRefBook(this);
        result.setStatus(RefBookVersionStatus.DRAFT);

        return result;
    }

    @Override
    public boolean isChangeableVersion(RefBookVersionEntity version) {

        return version != null &&
                RefBookVersionStatus.DRAFT.equals(version.getStatus());
    }
}

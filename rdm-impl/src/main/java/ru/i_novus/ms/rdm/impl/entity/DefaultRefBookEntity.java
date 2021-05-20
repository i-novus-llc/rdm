package ru.i_novus.ms.rdm.impl.entity;

import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

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
}

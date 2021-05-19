package ru.i_novus.ms.rdm.impl.entity;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RefBookTypeEnum.VALUES.DEFAULT)
public class DefaultRefBookEntity extends RefBookEntity {

    public DefaultRefBookEntity() {
        this.setType(RefBookTypeEnum.DEFAULT);
    }
}

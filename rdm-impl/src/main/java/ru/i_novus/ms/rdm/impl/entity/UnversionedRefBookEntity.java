package ru.i_novus.ms.rdm.impl.entity;

import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RefBookTypeEnum.VALUES.UNVERSIONED)
public class UnversionedRefBookEntity extends RefBookEntity {

    public UnversionedRefBookEntity() {
        this.setType(RefBookTypeEnum.UNVERSIONED);
    }
}

package ru.i_novus.ms.rdm.impl.entity;

import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue(RefBookTypeEnum.VALUES.UNVERSIONED)
public class UnversionedRefBookEntity extends RefBookEntity {

    private static final String USED_VERSION = "-1.0";

    public UnversionedRefBookEntity() {
        super(RefBookTypeEnum.UNVERSIONED);
    }

    @Override
    public RefBookVersionEntity createChangeableVersion() {

        RefBookVersionEntity result = new RefBookVersionEntity();
        result.setRefBook(this);
        result.setStatus(RefBookVersionStatus.PUBLISHED);
        result.setVersion(USED_VERSION);
        result.setFromDate(TimeUtils.now());

        return result;
    }

    @Override
    public boolean isChangeableVersion(RefBookVersionEntity version) {

        return version != null &&
                version.getRefBook() != null &&
                RefBookTypeEnum.UNVERSIONED.equals(version.getRefBook().getType());
    }
}

package ru.i_novus.ms.rdm.impl.entity;

import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import java.time.LocalDateTime;

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

    @Override
    public LocalDateTime getPublishedDate(RefBookVersionEntity publishedVersion) {

        return publishedVersion != null ? publishedVersion.getFromDate() : null;
    }
}

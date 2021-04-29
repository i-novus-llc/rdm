package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refbook.RefBookTypeEnum;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;

@Component
public class UnversionedValidateDraftExistsStrategy extends DefaultValidateDraftExistsStrategy {

    @Override
    protected boolean isDraft(RefBookVersionEntity entity) {

        return super.isDraft(entity) || isUnversioned(entity);
    }

    protected boolean isUnversioned(RefBookVersionEntity entity) {

        return entity != null &&
                entity.getRefBook() != null &&
                RefBookTypeEnum.UNVERSIONED.equals(entity.getRefBook().getType());
    }
}

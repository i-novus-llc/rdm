package ru.i_novus.ms.rdm.impl.strategy.draft;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.impl.entity.RefBookEntity;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

@Component
public class DefaultFindDraftEntityStrategy implements FindDraftEntityStrategy {

    @Autowired
    private RefBookVersionRepository versionRepository;

    @Override
    public RefBookVersionEntity find(RefBookEntity refBookEntity) {

        return versionRepository.findByStatusAndRefBookId(RefBookVersionStatus.DRAFT, refBookEntity.getId());
    }
}

package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.ms.rdm.impl.strategy.data.api.DeleteAllRowValuesStrategy;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

@Component
public class DefaultDeleteAllRowValuesStrategy implements DeleteAllRowValuesStrategy {

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private DraftDataService draftDataService;

    @Override
    public void deleteAll(RefBookVersionEntity entity) {

        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIsNotNull(entity.getId());

        draftDataService.deleteAllRows(entity.getStorageCode());
    }
}

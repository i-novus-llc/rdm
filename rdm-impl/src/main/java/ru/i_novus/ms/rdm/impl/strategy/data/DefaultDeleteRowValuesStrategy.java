package ru.i_novus.ms.rdm.impl.strategy.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.repository.RefBookConflictRepository;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.List;

import static ru.i_novus.ms.rdm.api.util.RowUtils.toLongSystemIds;

@Component
public class DefaultDeleteRowValuesStrategy implements DeleteRowValuesStrategy {

    @Autowired
    private RefBookConflictRepository conflictRepository;

    @Autowired
    private DraftDataService draftDataService;

    @Override
    public void delete(RefBookVersionEntity entity, List<Object> systemIds) {

        before(entity, systemIds);

        draftDataService.deleteRows(entity.getStorageCode(), systemIds);

        after(entity, systemIds);
    }

    protected void before(RefBookVersionEntity entity, List<Object> systemIds) {

        conflictRepository.deleteByReferrerVersionIdAndRefRecordIdIn(entity.getId(), toLongSystemIds(systemIds));
    }

    protected void after(RefBookVersionEntity entity, List<Object> systemIds) {

        // Nothing to do.
    }
}

package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Conflict;
import ru.inovus.ms.rdm.model.Draft;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.List;

import static java.util.Collections.emptyList;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private VersionService versionService;
    private DraftService draftService;
    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    private static final String VERSION_NOT_FOUND = "version.not.found";
    private static final String CONFLICTED_ROW_NOT_FOUND = "conflicted.row.not.found";
    private static final String CONFLICTED_REFERENCE_ROW_NOT_FOUND = "conflicted.reference.row.not.found";

    @Autowired
    public ConflictServiceImpl(VersionService versionService,
                               DraftService draftService,
                               DraftDataService draftDataService,
                               RefBookVersionRepository versionRepository) {
        this.versionService = versionService;
        this.draftService = draftService;
        this.draftDataService = draftDataService;
        this.versionRepository = versionRepository;
    }

    @Override
    public List<Conflict> calculateConflicts(Integer refFromId, Integer refToId) {
        validateVersionsExistence(refFromId);
        validateVersionsExistence(refToId);

        return emptyList();
    }

    /**
     * Получение конфликтной записи по конфликту.
     */
    private RefBookRowValue getRefToRowValue(RefBookVersion version, Conflict conflict) {

        // 1. Convert conflict to SearchDataCriteria
        // 2. Call versionService.search
        return null;
    }

    /**
     * Получение записей со ссылками на конфликтную запись по конфликту.
     */
    private Page<RefBookRowValue> getRefFromRowValues(RefBookVersion version, Conflict conflict) {

        // 1. Convert conflict to SearchDataCriteria
        // 2. Call versionService.search
        return null;
    }

    public void updateReferenceValues(Integer refFromId, Integer refToId, List<Conflict> conflicts) {

        if (CollectionUtils.isEmpty(conflicts))
            return;

        validateVersionsExistence(refFromId);
        validateVersionsExistence(refToId);

        RefBookVersion refFromVersion = versionService.getById(refFromId);
        RefBookVersion refToVersion = versionService.getById(refToId);

        Draft refFromDraft;
        if (RefBookVersionStatus.DRAFT.equals(refFromVersion.getStatus()))
            refFromDraft = draftService.getDraft(refFromId);
        else
            refFromDraft = draftService.createFromVersion(refFromId);

        conflicts.forEach(conflict -> {
            RefBookRowValue refToRow = getRefToRowValue(refToVersion, conflict);
            if (refToRow == null)
                throw new RdmException(CONFLICTED_ROW_NOT_FOUND);

            Page<RefBookRowValue> refFromRows = getRefFromRowValues(refFromVersion, conflict);
            if (refFromRows == null || refFromRows.isEmpty())
                throw new RdmException(CONFLICTED_REFERENCE_ROW_NOT_FOUND);

            refFromRows.forEach(refBookRowValue -> {
                // 1. Recalculate and update reference displayValue.
                // 2. Clear refBookRowValue.id to add only.
                // 3. Add or update row in refFromDraft.

                // NB: Code from DraftServiceImpl.java:updateData for refFromId.
//            if (refFromDraft.getId().equals(refFromId))
//                draftDataService.updateRow(refFromDraft.getStorageCode(), rowValue);
//            else
//                draftDataService.addRows(refFromDraft.getStorageCode(), singletonList(rowValue));
            });
        });
    }

    private void validateVersionsExistence(Integer versionId) {
        if (versionId == null || !versionRepository.existsById(versionId))
            throw new NotFoundException(new Message(VERSION_NOT_FOUND, versionId));
    }
}

package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.i18n.Message;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.enumeration.ConflictType;
import ru.inovus.ms.rdm.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.ArrayList;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;

@Primary
@Service
public class ConflictServiceImpl implements ConflictService {

    private RefBookService refBookService;
    private VersionService versionService;
    private DraftService draftService;
    private DraftDataService draftDataService;

    private RefBookVersionRepository versionRepository;

    private static final String VERSION_NOT_FOUND = "version.not.found";
    private static final String CONFLICTED_ROW_NOT_FOUND = "conflicted.row.not.found";
    private static final String CONFLICTED_REFERENCE_ROW_NOT_FOUND = "conflicted.reference.row.not.found";

    @Autowired
    public ConflictServiceImpl(RefBookService refBookService,
                               VersionService versionService,
                               DraftService draftService,
                               DraftDataService draftDataService,
                               RefBookVersionRepository versionRepository) {
        this.refBookService = refBookService;
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

        if (version == null || conflict == null ||
                CollectionUtils.isEmpty(conflict.getPrimaryKeys()))
            return null;

        // Convert conflict to criteria.
        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        conflict.getPrimaryKeys().forEach(fieldValue -> {
            FieldType fieldType = version.getStructure().getAttribute(fieldValue.getField()).getType();
            filters.add(new AttributeFilter(fieldValue.getField(), fieldValue.getValue(), fieldType));
        });
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(version.getId(), criteria);
        return (rowValues != null && !rowValues.isEmpty()) ? rowValues.get().findFirst().orElse(null) : null;
    }

    /**
     * Получение записей со ссылками на конфликтную запись по конфликту.
     */
    private Page<RefBookRowValue> getRefFromRowValues(RefBookVersion version, Conflict conflict,
                                                      Structure.Reference reference, String value) {

        if (version == null || conflict == null ||
                CollectionUtils.isEmpty(conflict.getPrimaryKeys()))
            return null;

        // Convert conflict to criteria.
        SearchDataCriteria criteria = new SearchDataCriteria();

        List<AttributeFilter> filters = new ArrayList<>();
        AttributeFilter filter = new AttributeFilter(reference.getAttribute(), value, FieldType.STRING, SearchTypeEnum.EXACT);
        filters.add(filter);
        criteria.setAttributeFilter(singleton(filters));

        Page<RefBookRowValue> rowValues = versionService.search(version.getId(), criteria);
        return (rowValues != null && !rowValues.isEmpty()) ? rowValues : null;
    }

    public void updateReferenceValues(RefBookVersion refFromDraft,
                                      RefBookVersion refToVersion,
                                      String refToBookCode,
                                      Conflict conflict) {

        if (conflict == null ||
                CollectionUtils.isEmpty(conflict.getPrimaryKeys()))
            return;

        RefBookRowValue refToRow = getRefToRowValue(refToVersion, conflict);
        if (refToRow == null)
            throw new RdmException(CONFLICTED_ROW_NOT_FOUND);

        String primaryValue = conflict.getPrimaryKeys().get(0).getValue().toString();

        List<Structure.Reference> references = refFromDraft.getStructure().getRefCodeReferences(refToBookCode);
        references.forEach(reference -> {
            Page<RefBookRowValue> refFromRows = getRefFromRowValues(refFromDraft, conflict, reference, primaryValue);
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

    public void updateReferenceValues(Integer refFromId, Integer refToId, List<Conflict> conflicts) {

        if (CollectionUtils.isEmpty(conflicts))
            return;

        validateVersionsExistence(refFromId);
        validateVersionsExistence(refToId);

        RefBookVersion refFromVersion = versionService.getById(refFromId);
        RefBookVersion refToVersion = versionService.getById(refToId);
        String refToBookCode = refBookService.getCode(refToVersion.getRefBookId());

        Draft updatingDraft;
        if (RefBookVersionStatus.DRAFT.equals(refFromVersion.getStatus()))
            updatingDraft = draftService.getDraft(refFromId);
        else
            updatingDraft = draftService.createFromVersion(refFromId);
        RefBookVersion refFromDraft = versionService.getById(updatingDraft.getId());

        conflicts.stream()
                .filter(conflict -> ConflictType.UPDATED.equals(conflict.getConflictType()))
                .forEach(conflict -> updateReferenceValues(refFromDraft, refToVersion, refToBookCode, conflict));
    }

    private void validateVersionsExistence(Integer versionId) {
        if (versionId == null || !versionRepository.existsById(versionId))
            throw new NotFoundException(new Message(VERSION_NOT_FOUND, versionId));
    }
}

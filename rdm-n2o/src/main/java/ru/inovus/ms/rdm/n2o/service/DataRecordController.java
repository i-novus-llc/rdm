package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DataConstants;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.inovus.ms.rdm.api.enumeration.ConflictType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.AttributeFilter;
import ru.inovus.ms.rdm.api.service.ConflictService;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.VersionService;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.api.util.TimeUtils.parseLocalDate;
import static ru.inovus.ms.rdm.n2o.provider.DataRecordQueryProvider.REFERENCE_CONFLICT_TEXT;
import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.addFieldPart;
import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.addPrefix;

@Controller
@SuppressWarnings("unused")
public class DataRecordController {

    private static final String CONFLICT_TEXT = "conflict.text";
    private static final String CONFLICT_TEXT_UPDATED = "conflict.text.updated";
    private static final String CONFLICT_TEXT_DELETED = "conflict.text.deleted";
    private static final String CONFLICT_TEXT_ALTERED = "conflict.text.altered";

    @Autowired
    Messages messages;

    @Autowired
    private VersionService versionService;

    @Autowired
    private DraftService draftService;

    @Autowired
    private ConflictService conflictService;

    public Map<String, Object> getRow(Integer versionId, Integer sysRecordId) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordIdFilter = new AttributeFilter(DataConstants.SYS_PRIMARY_COLUMN, sysRecordId, FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordIdFilter)));

        Page<RefBookRowValue> search = versionService.search(versionId, criteria);
        if (isEmpty(search.getContent()))
            return emptyMap();

        return getRow(versionId, sysRecordId, search.getContent().get(0));
    }

    public Map<String, Object> getRow(Integer versionId, Integer sysRecordId, LongRowValue rowValue) {

        Map<String, Object> map = new HashMap<>();
        map.put("id", sysRecordId);
        map.put("versionId", versionId);

        rowValue.getFieldValues().forEach(
                fieldValue -> map.put(addPrefix(fieldValue.getField()), fieldValue.getValue())
        );

        final Structure structure = versionService.getStructure(versionId);
        if (!isEmpty(structure.getReferences())) {
            List<RefBookConflict> conflicts = findDataConflicts(versionId, Long.valueOf(sysRecordId), structure.getReferences());

            if (!isEmpty(conflicts)) {
                structure.getReferences().forEach(reference -> {
                    ConflictType conflictType = conflicts.stream()
                            .filter(conflict -> reference.getAttribute().equals(conflict.getRefFieldCode()))
                            .map(RefBookConflict::getConflictType)
                            .findFirst().orElse(null);
                    String conflictTextName = addFieldPart(addPrefix(reference.getAttribute()), REFERENCE_CONFLICT_TEXT);
                    map.put(conflictTextName, conflictType != null ? getConflictText(conflictType) : null);
                });
            }
        }

        return map;
    }

    /**
     * Поиск конфликта по ссылаемой версии, идентификатору строки и ссылкам.
     *
     * @param versionId   идентификатор версии
     * @param rowSystemId идентификатор строки
     * @param references  список ссылок структуры
     * @return Список конфликтов
     */
    private List<RefBookConflict> findDataConflicts(Integer versionId, Long rowSystemId,
                                                    List<Structure.Reference> references) {

        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(versionId);
        criteria.setIsLastPublishedVersion(true);
        criteria.setRefFieldCodes(references.stream().map(Structure.Reference::getAttribute).collect(toList()));
        criteria.setRefRecordId(rowSystemId);
        criteria.setPageSize(references.size());

        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return (conflicts != null) ? conflicts.getContent() : emptyList();
    }

    private String getConflictText(ConflictType type) {

        String typeText = getConflictTypeText(type);
        if (typeText == null)
            return null;

        return messages.getMessage(CONFLICT_TEXT, messages.getMessage(typeText));
    }

    private String getConflictTypeText(ConflictType type) {
        switch (type) {
            case UPDATED: return CONFLICT_TEXT_UPDATED;
            case DELETED: return CONFLICT_TEXT_DELETED;
            case ALTERED: return CONFLICT_TEXT_ALTERED;
            default: return null;
        }
    }

    @SuppressWarnings("WeakerAccess")
    public void updateData(Integer draftId, Row row) {
        row.getData().entrySet().stream()
                .filter(e -> e.getValue() instanceof Date)
                .forEach(e -> e.setValue(parseLocalDate(e.getValue())));

        draftService.updateData(draftId, row);
    }
}

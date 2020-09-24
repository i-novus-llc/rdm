package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.i18n.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.util.StructureUtils;

import java.util.List;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Controller
public class UpdateRecordController {

    private static final Logger logger = LoggerFactory.getLogger(UpdateRecordController.class);

    private static final String CONFLICT_TEXT = "conflict.text";
    private static final String CONFLICT_TEXT_UPDATED = "conflict.text.updated";
    private static final String CONFLICT_TEXT_DELETED = "conflict.text.deleted";
    private static final String CONFLICT_TEXT_ALTERED = "conflict.text.altered";

    @Autowired
    private VersionRestService versionService;

    @Autowired
    private ConflictService conflictService;

    @Autowired
    private Messages messages;

    /**
     * Проверка наличия конфликтов для записи у ссылающейся версии.
     *
     * @param versionId идентификатор версии, которая ссылается
     * @param id        идентификатор записи этой версии
     * @return Строка со всеми конфликтами
     */
    @SuppressWarnings("unused") // used in: UpdateRecordQueryResolver
    public String getDataConflicts(Integer versionId, Long id) {

        final Structure structure = getStructureOrNull(versionId);
        if (structure == null || isEmpty(structure.getReferences()))
            return null;

        List<String> refFieldCodes = StructureUtils.getReferenceAttributeCodes(structure).collect(toList());
        List<RefBookConflict> conflicts = findDataConflicts(versionId, id, refFieldCodes);
        if (isEmpty(conflicts))
            return null;

        return conflicts.stream()
                .map(conflict -> getConflictText(conflict.getConflictType(),
                        () -> getConflictRefFieldName(conflict, structure)))
                .collect(joining(" \n"));
    }

    private Structure getStructureOrNull(Integer versionId) {
        try {
            return versionService.getStructure(versionId);

        } catch (Exception e) {
            logger.error("Structure is not received for data", e);

            return null;
        }
    }

    /**
     * Поиск конфликта по ссылающейся версии, идентификатору строки и ссылкам.
     *
     * @param versionId     идентификатор версии, которая ссылается
     * @param id            идентификатор записи этой версии
     * @param refFieldCodes список кодов ссылок в структуре этой версии
     * @return Список конфликтов
     */
    private List<RefBookConflict> findDataConflicts(Integer versionId,
                                                    Long id,
                                                    List<String> refFieldCodes) {
        RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(versionId);
        criteria.setIsLastPublishedVersion(true);
        criteria.setRefFieldCodes(refFieldCodes);
        criteria.setRefRecordId(id);
        criteria.setPageSize(refFieldCodes.size());

        Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return (conflicts != null) ? conflicts.getContent() : emptyList();
    }

    /** Получение названия атрибута с конфликтом. */
    private String getConflictRefFieldName(RefBookConflict conflict, Structure structure) {

        Structure.Attribute attribute = structure.getAttribute(conflict.getRefFieldCode());
        return attribute != null ? attribute.getName() : null;
    }

    /** Получение описания конфликта для атрибута. */
    private String getConflictText(ConflictType type, Supplier<String> attributeName) {

        String typeText = getConflictTypeText(type);
        if (typeText == null)
            return null;

        return messages.getMessage(CONFLICT_TEXT, attributeName.get(), messages.getMessage(typeText));
    }

    /** Получение описания конфликта по его типу. */
    private String getConflictTypeText(ConflictType type) {
        switch (type) {
            case UPDATED: return CONFLICT_TEXT_UPDATED;
            case DELETED: return CONFLICT_TEXT_DELETED;
            case ALTERED: return CONFLICT_TEXT_ALTERED;
            default: return null;
        }
    }
}

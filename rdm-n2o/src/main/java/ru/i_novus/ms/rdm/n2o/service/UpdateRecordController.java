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

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.util.StringUtils.joinNumerated;

@Controller
public class UpdateRecordController {

    private static final Logger logger = LoggerFactory.getLogger(UpdateRecordController.class);

    private static final String CONFLICT_TEXT = "conflict.text";
    private static final String CONFLICT_TEXT_UPDATED = "conflict.text.updated";
    private static final String CONFLICT_TEXT_DELETED = "conflict.text.deleted";
    private static final String CONFLICT_TEXT_ALTERED = "conflict.text.altered";

    private final VersionRestService versionService;

    private final ConflictService conflictService;

    private final Messages messages;

    @Autowired
    public UpdateRecordController(VersionRestService versionService,
                                  ConflictService conflictService,
                                  Messages messages) {

        this.versionService = versionService;
        this.conflictService = conflictService;

        this.messages = messages;
    }

    /**
     * Проверка наличия конфликтов для записи у ссылающейся версии.
     *
     * @param versionId идентификатор версии, которая ссылается
     * @param id        идентификатор записи этой версии
     * @return Строка со всеми конфликтами
     */
    @SuppressWarnings("unused") // used in: UpdateRecordQueryResolver
    public String getDataConflicts(Integer versionId, Long id) {

        final Structure structure = getStructureOrEmpty(versionId);
        if (structure.isEmpty() || structure.getReferences().isEmpty())
            return null;

        final List<String> refFieldCodes = structure.getReferenceAttributeCodes();
        final List<RefBookConflict> conflicts = findDataConflicts(versionId, id, refFieldCodes);
        if (isEmpty(conflicts))
            return null;

        final List<String> conflictTexts = conflicts.stream()
                .map(conflict -> getConflictText(conflict, structure))
                .filter(Objects::nonNull)
                .collect(toList());

        return joinNumerated(conflictTexts);
    }

    private Structure getStructureOrEmpty(Integer versionId) {
        try {
            return versionService.getStructure(versionId);

        } catch (Exception e) {
            logger.error("Structure is not received for data", e);

            return Structure.EMPTY;
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
        final RefBookConflictCriteria criteria = new RefBookConflictCriteria();
        criteria.setReferrerVersionId(versionId);
        criteria.setIsLastPublishedVersion(true);
        criteria.setRefFieldCodes(refFieldCodes);
        criteria.setRefRecordId(id);
        criteria.setPageSize(refFieldCodes.size());

        final Page<RefBookConflict> conflicts = conflictService.search(criteria);
        return (conflicts != null) ? conflicts.getContent() : emptyList();
    }

    /** Получение описания конфликта. */
    private String getConflictText(RefBookConflict conflict, Structure structure) {

        return getConflictText(conflict.getConflictType(), () -> getConflictRefFieldName(conflict, structure));
    }

    /** Получение наименования атрибута с конфликтом. */
    private String getConflictRefFieldName(RefBookConflict conflict, Structure structure) {

        final Structure.Attribute attribute = structure.getAttribute(conflict.getRefFieldCode());
        return attribute != null ? attribute.getName() : null;
    }

    /** Получение описания конфликта для атрибута. */
    private String getConflictText(ConflictType type, Supplier<String> attributeName) {

        final String typeText = getConflictTypeText(type);
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

package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.util.StructureUtils;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates;
import ru.inovus.ms.rdm.impl.repository.RefBookRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;
import ru.inovus.ms.rdm.impl.util.NamingUtils;

import java.util.List;

import static org.springframework.util.StringUtils.isEmpty;

@Component
public class VersionValidationImpl implements VersionValidation {

    public static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";
    public static final String REFBOOK_WITH_CODE_NOT_FOUND_EXCEPTION_CODE = "refbook.with.code.not.found";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    public static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";
    public static final String REFBOOK_IS_ARCHIVED_EXCEPTION_CODE = "refbook.is.archived";
    private static final String VERSION_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "version.attribute.not.found";
    private static final String DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "draft.attribute.not.found";

    private static final String REFERENCE_REFERRED_ATTRIBUTE_NOT_FOUND = "reference.referred.attribute.not.found";
    private static final String REFERENCE_REFERRED_ATTRIBUTES_NOT_FOUND = "reference.referred.attributes.not.found";

    private RefBookRepository refbookRepository;
    private RefBookVersionRepository versionRepository;

    @Autowired
    public VersionValidationImpl(RefBookRepository refbookRepository,
                                 RefBookVersionRepository versionRepository) {
        this.refbookRepository = refbookRepository;
        this.versionRepository = versionRepository;
    }

    /**
     * Общая проверка справочника.
     *
     * @param refBookId идентификатор справочника
     */
    @Override
    public void validateRefBook(Integer refBookId) {
        validateRefBookExists(refBookId);
        validateRefBookNotArchived(refBookId);
    }

    /**
     * Общая проверка версии справочника.
     *
     * @param versionId идентификатор версии
     */
    @Override
    public void validateVersion(Integer versionId) {
        validateVersionExists(versionId);
        validateVersionNotArchived(versionId);
    }

    /**
     * Общая проверка черновика справочника.
     *
     * @param draftId идентификатор черновика
     */
    @Override
    public void validateDraft(Integer draftId) {
        validateDraftExists(draftId);
        validateDraftNotArchived(draftId);
    }

    /**
     * Проверка существования справочника.
     *
     * @param refBookId идентификатор справочника
     */
    @Override
    public void validateRefBookExists(Integer refBookId) {
        if (refBookId == null || !refbookRepository.existsById(refBookId)) {
            throw new NotFoundException(new Message(REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));
        }
    }

    /**
     * Проверка существования справочника по коду.
     *
     * @param refBookCode код справочника
     */
    @Override
    public void validateRefBookCodeExists(String refBookCode) {
        if (isEmpty(refBookCode) || !refbookRepository.existsByCode(refBookCode)) {
            throw new NotFoundException(new Message(REFBOOK_WITH_CODE_NOT_FOUND_EXCEPTION_CODE, refBookCode));
        }
    }

    /**
     * Проверка существования версии справочника.
     *
     * @param versionId идентификатор версии
     */
    @Override
    public void validateVersionExists(Integer versionId) {
        if (versionId == null || !versionRepository.exists(RefBookVersionPredicates.hasVersionId(versionId))) {
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));
        }
    }

    /**
     * Проверка существования черновика справочника.
     *
     * @param draftId идентификатор черновика
     */
    @Override
    public void validateDraftExists(Integer draftId) {
        if (draftId == null || !versionRepository.exists(RefBookVersionPredicates.hasVersionId(draftId).and(RefBookVersionPredicates.isDraft()))) {
            throw new NotFoundException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, draftId));
        }
    }

    /**
     * Проверка наличия справочника не в архиве.
     *
     * @param refBookId идентификатор справочника
     */
    private void validateRefBookNotArchived(Integer refBookId) {
        if (refBookId != null && versionRepository.exists(RefBookVersionPredicates.isVersionOfRefBook(refBookId).and(RefBookVersionPredicates.isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка наличия версии справочника не в архиве.
     *
     * @param versionId идентификатор версии
     */
    private void validateVersionNotArchived(Integer versionId) {
        if (versionId != null && versionRepository.exists(RefBookVersionPredicates.hasVersionId(versionId).and(RefBookVersionPredicates.isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка наличия черновика справочника не в архиве.
     *
     * @param draftId идентификатор черновика
     */
    public void validateDraftNotArchived(Integer draftId) {
        if (draftId != null && versionRepository.exists(RefBookVersionPredicates.hasVersionId(draftId).and(RefBookVersionPredicates.isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка существования атрибута в структуре версии справочника.
     *
     * @param versionId идентификатор версии
     * @param structure структура версии
     * @param attribute код атрибута
     */
    @Override
    public void validateAttributeExists(Integer versionId, Structure structure, String attribute) {
        if (structure.getAttribute(attribute) == null) {
            throw new NotFoundException(new Message(VERSION_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE, versionId, attribute));
        }
    }

    /**
     * Проверка существования атрибута версии справочника.
     *
     * @param versionId идентификатор версии
     * @param attribute код атрибута
     */
    @Override
    public void validateDraftAttributeExists(Integer versionId, String attribute) {

        validateDraftExists(versionId);

        Structure structure = versionRepository.getOne(versionId).getStructure();
        if (structure.getAttribute(attribute) == null) {
            throw new NotFoundException(new Message(DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE, versionId, attribute));
        }
    }

    /** Проверка структуры.
     *
     * @param structure структура версии справочника
     */
    public void validateStructure(Structure structure) {
        if (structure == null
                || structure.getAttributes() == null)
            return;

        structure.getAttributes().forEach(attr -> NamingUtils.checkCode(attr.getCode()));
    }

    /**
     * Проверка выражения для вычисления отображаемого ссылочного значения.
     *
     * @param displayExpression выражение для вычисления отображаемого ссылочного значения
     * @param referredVersion   версия справочника, на который ссылаются
     */
    @Override
    public void validateReferenceDisplayExpression(String displayExpression,
                                                   RefBookVersion referredVersion) {
        if (isEmpty(displayExpression))
            return; // NB: to-do: throw exception and fix absent referredBook in testLifecycle.

        List<String> incorrectFields = StructureUtils.getAbsentPlaceholders(displayExpression, referredVersion.getStructure());
        if (!CollectionUtils.isEmpty(incorrectFields)) {
            if (incorrectFields.size() == 1)
                throw new UserException(new Message(REFERENCE_REFERRED_ATTRIBUTE_NOT_FOUND, incorrectFields.get(0)));

            String incorrectCodes = String.join("\",\"", incorrectFields);
            throw new UserException(new Message(REFERENCE_REFERRED_ATTRIBUTES_NOT_FOUND, incorrectCodes));
        }
    }
}

package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.inovus.ms.rdm.exception.NotFoundException;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;

import static ru.inovus.ms.rdm.repositiory.RefBookVersionPredicates.*;

@Component
public class VersionValidationImpl implements VersionValidation {

    public static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    public static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";
    public static final String REFBOOK_IS_ARCHIVED_EXCEPTION_CODE = "refbook.is.archived";
    private static final String DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "draft.attribute.not.found";

    private RefBookVersionRepository versionRepository;

    @Autowired
    public VersionValidationImpl(RefBookVersionRepository versionRepository) {
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
        if (refBookId == null || !versionRepository.exists(isVersionOfRefBook(refBookId))) {
            throw new NotFoundException(new Message(REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));
        }
    }

    /**
     * Проверка существования версии справочника.
     *
     * @param versionId идентификатор версии
     */
    @Override
    public void validateVersionExists(Integer versionId) {
        if (versionId == null || !versionRepository.exists(hasVersionId(versionId))) {
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
        if (draftId == null || !versionRepository.exists(hasVersionId(draftId).and(isDraft()))) {
            throw new NotFoundException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, draftId));
        }
    }

    /**
     * Проверка наличия справочника не в архиве.
     *
     * @param refBookId идентификатор справочника
     */
    private void validateRefBookNotArchived(Integer refBookId) {
        if (refBookId != null && versionRepository.exists(isVersionOfRefBook(refBookId).and(isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка наличия версии справочника не в архиве.
     *
     * @param versionId идентификатор версии
     */
    private void validateVersionNotArchived(Integer versionId) {
        if (versionId != null && versionRepository.exists(hasVersionId(versionId).and(isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка наличия черновика справочника не в архиве.
     *
     * @param draftId идентификатор черновика
     */
    private void validateDraftNotArchived(Integer draftId) {
        if (draftId != null && versionRepository.exists(hasVersionId(draftId).and(isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка существования атрибута версии справочника.
     *
     * @param versionId идентификатор версии
     * @param attribute код атрибута
     */
    @Override
    public void validateAttributeExists(Integer versionId, String attribute) {
        validateDraftExists(versionId);

        Structure structure = versionRepository.getOne(versionId).getStructure();
        if (structure.getAttribute(attribute) == null) {
            throw new NotFoundException(new Message(DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE, versionId, attribute));
        }
    }
}

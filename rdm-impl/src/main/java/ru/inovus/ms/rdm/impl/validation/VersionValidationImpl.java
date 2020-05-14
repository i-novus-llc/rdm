package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.apache.cxf.common.util.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import ru.inovus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.inovus.ms.rdm.api.exception.NotFoundException;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.util.StructureUtils;
import ru.inovus.ms.rdm.api.validation.VersionValidation;
import ru.inovus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.impl.predicate.RefBookVersionPredicates;
import ru.inovus.ms.rdm.impl.repository.RefBookRepository;
import ru.inovus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;

@Component
// Использовать VersionValidation без интерфейса как ReferenceValidation
// Выделить RefBookValidation с refbookRepository.
public class VersionValidationImpl implements VersionValidation {

    public static final String CODE_IS_INVALID_EXCEPTION_CODE = "code.is.invalid";
    public static final String REFBOOK_CODE_IS_INVALID_EXCEPTION_CODE = "refbook.code.is.invalid";
    public static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";
    public static final String REFBOOK_WITH_CODE_NOT_FOUND_EXCEPTION_CODE = "refbook.with.code.not.found";
    private static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    public static final String DRAFT_NOT_FOUND_EXCEPTION_CODE = "draft.not.found";
    public static final String REFBOOK_IS_ARCHIVED_EXCEPTION_CODE = "refbook.is.archived";
    private static final String VERSION_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "version.attribute.not.found";
    private static final String DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "draft.attribute.not.found";

    public static final String ATTRIBUTE_CODE_IS_INVALID_EXCEPTION_CODE = "attribute.code.is.invalid";
    public static final String ATTRIBUTE_REFERENCE_NOT_FOUND_EXCEPTION_CODE = "attribute.reference.not.found";
    public static final String REFERENCE_ATTRIBUTE_CANNOT_BE_PRIMARY_KEY_EXCEPTION_CODE = "reference.attribute.cannot.be.primary.key";
    public static final String REFERENCE_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "reference.attribute.not.found";
    public static final String REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE = "reference.book.must.have.primary.key";
    public static final String REFERENCE_STRUCTURE_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE = "reference.requires.primary.key";
    private static final String REFERENCE_REFERRED_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "reference.referred.attribute.not.found";
    private static final String REFERENCE_DISPLAY_EXPRESSION_IS_EMPTY_EXCEPTION_CODE = "reference.display.expression.is.empty";
    private static final String REFERENCE_REFERRED_ATTRIBUTES_NOT_FOUND_EXCEPTION_CODE = "reference.referred.attributes.not.found";
    private static final String REFERRED_BOOK_NOT_FOUND_EXCEPTION_CODE = "referred.book.not.found";
    private static final String REFERRED_BOOK_STRUCTURE_NOT_FOUND_EXCEPTION_CODE = "referred.book.structure.not.found";
    private static final String REFERRED_BOOK_MUST_HAVE_ONLY_ONE_PRIMARY_KEY_EXCEPTION_CODE = "referred.book.must.have.only.one.primary.key";

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z][0-9A-Za-z\\-._]{0,49}");

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
     * Проверка кода справочника.
     *
     * @param code код справочника
     */
    @Override
    public void validateRefBookCode(String code) {

        if (!isValidCode(code)) {
            throw new UserException(List.of(
                    new Message(REFBOOK_CODE_IS_INVALID_EXCEPTION_CODE, code),
                    new Message(CODE_IS_INVALID_EXCEPTION_CODE)
            ));
        }
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

        if (StringUtils.isEmpty(refBookCode)
                || !refbookRepository.existsByCode(refBookCode)) {
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

        if (versionId == null
                || !versionRepository.exists(RefBookVersionPredicates.hasVersionId(versionId))) {
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

        if (draftId == null
                || !versionRepository.exists(RefBookVersionPredicates.hasVersionId(draftId).and(RefBookVersionPredicates.isDraft()))) {
            throw new NotFoundException(new Message(DRAFT_NOT_FOUND_EXCEPTION_CODE, draftId));
        }
    }

    /**
     * Проверка наличия справочника не в архиве.
     *
     * @param refBookId идентификатор справочника
     */
    private void validateRefBookNotArchived(Integer refBookId) {

        if (refBookId != null
                && versionRepository.exists(RefBookVersionPredicates.isVersionOfRefBook(refBookId).and(RefBookVersionPredicates.isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка наличия версии справочника не в архиве.
     *
     * @param versionId идентификатор версии
     */
    private void validateVersionNotArchived(Integer versionId) {

        if (versionId != null
                && versionRepository.exists(RefBookVersionPredicates.hasVersionId(versionId).and(RefBookVersionPredicates.isArchived()))) {
            throw new UserException(new Message(REFBOOK_IS_ARCHIVED_EXCEPTION_CODE));
        }
    }

    /**
     * Проверка наличия черновика справочника не в архиве.
     *
     * @param draftId идентификатор черновика
     */
    public void validateDraftNotArchived(Integer draftId) {

        if (draftId != null
                && versionRepository.exists(RefBookVersionPredicates.hasVersionId(draftId).and(RefBookVersionPredicates.isArchived()))) {
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
     * Используется при создании черновика с требуемой структурой.
     * Например, при создании справочника из файла и загрузке черновика из файла.
     *
     * @param structure структура версии справочника
     */
    @Override
    public void validateStructure(Structure structure) {

        validateAttributeStructure(structure);
        validateReferenceStructure(structure);
    }

    /** Проверка только атрибутов структуры.
     *
     * @param structure структура версии справочника
     */
    private void validateAttributeStructure(Structure structure) {
        if (structure == null
                || CollectionUtils.isEmpty(structure.getAttributes()))
            return;

        List<Message> errors = structure.getAttributes().stream()
                .filter(attribute -> attribute.isReferenceType()
                        && structure.getReference(attribute.getCode()) == null)
                .map(attribute -> new Message(ATTRIBUTE_REFERENCE_NOT_FOUND_EXCEPTION_CODE, attribute.getCode()))
                .collect(toList());
        if (!CollectionUtils.isEmpty(errors))
            throw new NotFoundException(errors);

        structure.getAttributes().forEach(this::validateAttribute);
    }

    /** Проверка атрибутов-ссылок структуры.
     *
     * @param structure структура версии справочника
     */
    private void validateReferenceStructure(Structure structure) {
        if (structure == null
                || CollectionUtils.isEmpty(structure.getReferences()))
            return;

        if (structure.isEmpty() || !structure.hasPrimary())
            throw new UserException(REFERENCE_STRUCTURE_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE);

        structure.getReferences().forEach(reference -> validateReference(reference, structure));
    }

    /**
     * Проверка атрибута.
     *
     * @param attribute атрибут
     */
    @Override
    public void validateAttribute(Structure.Attribute attribute) {

        validateAttributeCode(attribute.getCode());
    }

    /**
     * Проверка кода атрибута.
     *
     * @param code код атрибута
     */
    private void validateAttributeCode(String code) {

        if (!isValidCode(code)) {
            throw new UserException(List.of(
                    new Message(ATTRIBUTE_CODE_IS_INVALID_EXCEPTION_CODE, code),
                    new Message(CODE_IS_INVALID_EXCEPTION_CODE)
            ));
        }
    }

    /**
     * Проверка атрибута-ссылки.
     *
     * @param reference атрибут-ссылка
     */
    private void validateReference(Structure.Reference reference, Structure structure) {

        Structure.Attribute attribute = structure.getAttribute(reference.getAttribute());
        if (attribute == null)
            throw new NotFoundException(new Message(REFERENCE_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE, reference.getAttribute()));

        if (attribute.hasIsPrimary())
            throw new UserException(new Message(REFERENCE_ATTRIBUTE_CANNOT_BE_PRIMARY_KEY_EXCEPTION_CODE, attribute.getName()));

        validateReferenceCode(reference.getReferenceCode());
    }

    /**
     * Проверка кода справочника из ссылки.
     *
     * @param code код справочника из ссылки
     */
    private void validateReferenceCode(String code) {

        RefBookVersionEntity version = versionRepository
                .findFirstByRefBookCodeAndStatusOrderByFromDateDesc(code, RefBookVersionStatus.PUBLISHED);
        if (version == null)
            throw new UserException(new Message(REFERRED_BOOK_NOT_FOUND_EXCEPTION_CODE, code));
        if (version.getStructure() == null)
            throw new UserException(new Message(REFERRED_BOOK_STRUCTURE_NOT_FOUND_EXCEPTION_CODE, code));
    }

    /**
     * Проверка ссылочности перед добавлением/изменением.
     *
     * @param reference атрибут-ссылка
     */
    @Override
    public void validateReferenceAbility(Structure.Reference reference) {

        if (StringUtils.isEmpty(reference.getDisplayExpression()))
            throw new UserException(new Message(REFERENCE_DISPLAY_EXPRESSION_IS_EMPTY_EXCEPTION_CODE, reference.getAttribute()));

        RefBookVersionEntity referredEntity = versionRepository
                .findFirstByRefBookCodeAndStatusOrderByFromDateDesc(reference.getReferenceCode(), RefBookVersionStatus.PUBLISHED);
        if (referredEntity == null)
            throw new NotFoundException(new Message(REFBOOK_WITH_CODE_NOT_FOUND_EXCEPTION_CODE, reference.getReferenceCode()));

        if (referredEntity.getStructure().getPrimary().size() != 1)
            throw new UserException(new Message(REFERRED_BOOK_MUST_HAVE_ONLY_ONE_PRIMARY_KEY_EXCEPTION_CODE, reference.getReferenceCode()));

        validateReferenceDisplayExpression(reference, referredEntity.getStructure());
    }

    /**
     * Проверка выражения для вычисления отображаемого ссылочного значения.
     *
     * @param reference         атрибут-ссылка
     * @param referredStructure структура версии справочника, на который ссылаются
     */
    private void validateReferenceDisplayExpression(Structure.Reference reference, Structure referredStructure) {

        List<String> absents = StructureUtils.getAbsentPlaceholders(reference.getDisplayExpression(), referredStructure);
        if (CollectionUtils.isEmpty(absents))
            return;

        Message error;
        if (absents.size() == 1) {
            error = new Message(REFERENCE_REFERRED_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE,
                    reference.getAttribute(), absents.get(0));
        } else {
            error = new Message(REFERENCE_REFERRED_ATTRIBUTES_NOT_FOUND_EXCEPTION_CODE,
                    reference.getAttribute(), String.join("\",\"", absents));
        }
        throw new NotFoundException(error);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidCode(String code) {
        return CODE_PATTERN.matcher(code).matches();
    }
}

package ru.i_novus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import net.n2oapp.platform.i18n.UserException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.enumeration.RefBookSourceType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookStatusType;
import ru.i_novus.ms.rdm.api.enumeration.RefBookVersionStatus;
import ru.i_novus.ms.rdm.api.exception.NotFoundException;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.util.StringUtils;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.api.validation.VersionValidation;
import ru.i_novus.ms.rdm.impl.entity.RefBookVersionEntity;
import ru.i_novus.ms.rdm.impl.predicate.RefBookVersionPredicates;
import ru.i_novus.ms.rdm.impl.repository.RefBookRepository;
import ru.i_novus.ms.rdm.impl.repository.RefBookVersionRepository;

import java.util.List;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;

@Component
// Выделить RefBookValidation с refbookRepository.
public class VersionValidationImpl implements VersionValidation {

    public static final String CODE_IS_INVALID_EXCEPTION_CODE = "code.is.invalid";
    public static final String REFBOOK_CODE_IS_INVALID_EXCEPTION_CODE = "refbook.code.is.invalid";
    public static final String REFBOOK_NOT_FOUND_EXCEPTION_CODE = "refbook.not.found";
    public static final String REFBOOK_WITH_CODE_NOT_FOUND_EXCEPTION_CODE = "refbook.with.code.not.found";
    private static final String REFBOOK_WITH_ALREADY_EXISTS_EXCEPTION_CODE = "refbook.with.code.already.exists";
    public static final String VERSION_NOT_FOUND_EXCEPTION_CODE = "version.not.found";
    private static final String DRAFT_WAS_CHANGED_EXCEPTION_CODE = "draft.was.changed";
    public static final String LAST_PUBLISHED_NOT_FOUND_EXCEPTION_CODE = "last.published.not.found";
    public static final String REFBOOK_IS_ARCHIVED_EXCEPTION_CODE = "refbook.is.archived";
    public static final String REFBOOK_WITH_CODE_IS_ARCHIVED_EXCEPTION_CODE = "refbook.with.code.is.archived";
    private static final String VERSION_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "version.attribute.not.found";
    private static final String DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "draft.attribute.not.found";

    public static final String ATTRIBUTE_CODE_IS_INVALID_EXCEPTION_CODE = "attribute.code.is.invalid";
    public static final String ATTRIBUTE_REFERENCE_NOT_FOUND_EXCEPTION_CODE = "attribute.reference.not.found";
    public static final String REFERENCE_ATTRIBUTE_CANNOT_BE_PRIMARY_KEY_EXCEPTION_CODE = "reference.attribute.cannot.be.primary.key";
    public static final String REFERENCE_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "reference.attribute.not.found";
    public static final String REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE = "reference.book.must.have.primary.key";
    public static final String REFERENCE_STRUCTURE_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE = "reference.requires.primary.key";
    private static final String REFERENCE_DISPLAY_EXPRESSION_IS_EMPTY_EXCEPTION_CODE = "reference.display.expression.is.empty";
    private static final String REFERENCE_REFERRED_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE = "reference.referred.attribute.not.found";
    private static final String REFERENCE_REFERRED_ATTRIBUTES_NOT_FOUND_EXCEPTION_CODE = "reference.referred.attributes.not.found";
    private static final String REFERRED_BOOK_NOT_FOUND_EXCEPTION_CODE = "referred.book.not.found";
    private static final String REFERRED_BOOK_STRUCTURE_NOT_FOUND_EXCEPTION_CODE = "referred.book.structure.not.found";
    private static final String REFERRED_BOOK_HAS_NO_PRIMARY_EXCEPTION_CODE = "referred.book.has.no.primary";
    private static final String REFERRED_BOOK_HAS_MORE_PRIMARIES_EXCEPTION_CODE = "referred.book.has.more.primaries";
    private static final String REFERRED_DRAFT_PRIMARIES_NOT_MATCH_EXCEPTION_CODE = "referred.draft.primaries.not.match";

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Za-z][0-9A-Za-z\\-._]{0,49}");

    private final RefBookRepository refBookRepository;
    private final RefBookVersionRepository versionRepository;

    @Autowired
    public VersionValidationImpl(RefBookRepository refBookRepository,
                                 RefBookVersionRepository versionRepository) {
        this.refBookRepository = refBookRepository;
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
     * Проверка кода справочника.
     *
     * @param refBookCode код справочника
     */
    @Override
    public void validateRefBookCode(String refBookCode) {

        if (!isValidCode(refBookCode)) {
            throw new UserException(List.of(
                    new Message(REFBOOK_CODE_IS_INVALID_EXCEPTION_CODE, refBookCode),
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
        if (refBookId == null || !refBookRepository.existsById(refBookId)) {
            throw new NotFoundException(new Message(REFBOOK_NOT_FOUND_EXCEPTION_CODE, refBookId));
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
     * Проверка наличия справочника с указанным кодом.
     *
     * @param refBookCode код справочника
     */
    @Override
    public void validateRefBookCodeExists(String refBookCode) {

        if (StringUtils.isEmpty(refBookCode)
                || !refBookRepository.existsByCode(refBookCode)) {
            throw new NotFoundException(new Message(REFBOOK_WITH_CODE_NOT_FOUND_EXCEPTION_CODE, refBookCode));
        }
    }

    /**
     * Проверка отсутствия справочника с указанным кодом.
     *
     * @param refBookCode код справочника
     */
    @Override
    @SuppressWarnings("I-novus:MethodNameWordCountRule")
    public void validateRefBookCodeNotExists(String refBookCode) {

        if (StringUtils.isEmpty(refBookCode)
                        || refBookRepository.existsByCode(refBookCode))
            throw new UserException(new Message(REFBOOK_WITH_ALREADY_EXISTS_EXCEPTION_CODE, refBookCode));
    }

    /**
     * Проверка на наличие справочников, ссылающихся на указанный справочник.
     *
     * @param refBookCode код справочника
     * @return Результат проверки
     */
    @Override
    public boolean hasReferrerVersions(String refBookCode) {

        Boolean exists = versionRepository.existsReferrerVersions(refBookCode,
                RefBookStatusType.ALL.name(), RefBookSourceType.ALL.name());
        return Boolean.TRUE.equals(exists);
    }

    /**
     * Проверка существования версии справочника.
     *
     * @param versionId идентификатор версии
     */
    @Override
    public void validateVersionExists(Integer versionId) {

        if (versionId == null
                || !versionRepository.existsById(versionId)) {
            throw new NotFoundException(new Message(VERSION_NOT_FOUND_EXCEPTION_CODE, versionId));
        }
    }

    /**
     * Проверка значения оптимистической блокировки черновика справочника.
     *
     * @param draftId        идентификатор черновика
     * @param draftLockValue значение оптимистической блокировки черновика
     * @param optLockValue   проверяемое значение оптимистической блокировки версии
     */
    public void validateOptLockValue(Integer draftId, Integer draftLockValue, Integer optLockValue) {

        if (draftId != null && optLockValue != null
                && !optLockValue.equals(draftLockValue)) {
            throw new UserException(new Message(DRAFT_WAS_CHANGED_EXCEPTION_CODE, draftId));
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
     * Проверка существования атрибута черновика справочника.
     *
     * @param draftId   идентификатор черновика
     * @param structure структура черновика
     * @param attribute код атрибута
     */
    @Override
    public void validateDraftAttributeExists(Integer draftId, Structure structure, String attribute) {

        if (structure.getAttribute(attribute) == null)
            throw new NotFoundException(new Message(DRAFT_ATTRIBUTE_NOT_FOUND_EXCEPTION_CODE, draftId, attribute));
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
     * @param referredCode код справочника из ссылки
     */
    private void validateReferenceCode(String referredCode) {

        RefBookVersionEntity referredEntity = getReferredEntity(referredCode);

        if (referredEntity == null)
            throw new NotFoundException(new Message(REFERRED_BOOK_NOT_FOUND_EXCEPTION_CODE, referredCode));

        if (referredEntity.hasEmptyStructure()) // Только проверка на наличие структуры!
            throw new UserException(new Message(REFERRED_BOOK_STRUCTURE_NOT_FOUND_EXCEPTION_CODE, referredCode));
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

        RefBookVersionEntity referredEntity = getReferredEntity(reference.getReferenceCode());
        validateReferredStructure(referredEntity.getRefBook().getCode(), referredEntity.getStructure());

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

    /**
     * Проверка структуры черновика справочника.
     *
     * @param refBookCode    код справочника
     * @param draftStructure структура черновика этого справочника
     */
    @Override
    public void validateDraftStructure(String refBookCode, Structure draftStructure) {

        validateStructure(draftStructure);

        if (hasReferrerVersions(refBookCode)) {
            validateReferredDraftStructure(refBookCode, draftStructure);
        }
    }

    /**
     * Проверка структуры версии ссылочного справочника.
     *
     * @param structure структура версии справочника, который ссылается
     */
    @Override
    public void validateReferrerStructure(Structure structure) {

        if (structure == null
                || CollectionUtils.isEmpty(structure.getReferences()))
            return;

        if (structure.isEmpty() || !structure.hasPrimary())
            throw new UserException(REFERENCE_STRUCTURE_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE);

        structure.getReferences().forEach(this::validateReferenceAbility);
    }

    /**
     * Проверка структуры черновика справочника, на который ссылаются.
     *
     * @param referredCode   код справочника, на который ссылаются
     * @param draftStructure структура черновика этого справочника
     */
    private void validateReferredDraftStructure(String referredCode, Structure draftStructure) {

        validateRefBookCode(referredCode);

        validateReferredStructure(referredCode, draftStructure);

        RefBookVersionEntity referredEntity = getReferredEntity(referredCode);
        if (referredEntity == null)
            return;

        if (!equalsPrimaries(referredEntity.getStructure().getPrimaries(), draftStructure.getPrimaries()))
            throw new UserException(new Message(REFERRED_DRAFT_PRIMARIES_NOT_MATCH_EXCEPTION_CODE, referredCode, referredEntity.getVersion()));
    }

    /**
     * Получение версии справочника, на который ссылаются.
     *
     * @param referredCode код этого справочника
     * @return Версия справочника
     */
    private RefBookVersionEntity getReferredEntity(String referredCode) {

        validateRefBookCode(referredCode);

        return versionRepository
                .findFirstByRefBookCodeAndStatusOrderByFromDateDesc(referredCode, RefBookVersionStatus.PUBLISHED);
    }

    /**
     * Проверка структуры версии справочника, на который ссылаются.
     *
     * @param referredCode код этого справочника
     * @param structure    проверяемая структура
     */
    private void validateReferredStructure(String referredCode, Structure structure) {

        if (structure == null)
            throw new UserException(new Message(REFERRED_BOOK_STRUCTURE_NOT_FOUND_EXCEPTION_CODE, referredCode));

        int primaryCount = structure.getPrimaries().size();
        if (primaryCount == 0)
            throw new UserException(new Message(REFERRED_BOOK_HAS_NO_PRIMARY_EXCEPTION_CODE, referredCode));
        else
        if (primaryCount > 1)
            throw new UserException(new Message(REFERRED_BOOK_HAS_MORE_PRIMARIES_EXCEPTION_CODE, referredCode));
    }

    /**
     * Проверка атрибута перед добавлением в структуру.
     *
     * @param newAttribute добавляемый атрибут
     * @param oldStructure исходная структура
     * @param refBookCode  код справочника
     */
    @Override
    public void validateNewAttribute(Structure.Attribute newAttribute,
                                     Structure oldStructure, String refBookCode) {

        if (!newAttribute.hasIsPrimary()
                && !oldStructure.hasPrimary()
                && !isEmpty(oldStructure.getReferences())
                && !newAttribute.isReferenceType() // Только для добавления первичного ключа в старые справочники!
        )
            throw new UserException(new Message(REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));

        validateAttribute(newAttribute);
    }

    /**
     * Проверка атрибута-ссылки перед добавлением в структуру.
     *
     * @param newAttribute добавляемый атрибут
     * @param newReference добавляемый атрибут-ссылка
     * @param oldStructure исходная структура
     * @param refBookCode  код справочника
     */
    @Override
    public void validateNewReference(Structure.Attribute newAttribute,
                                     Structure.Reference newReference,
                                     Structure oldStructure, String refBookCode) {

        if (newAttribute.hasIsPrimary())
            throw new UserException(new Message(REFERENCE_ATTRIBUTE_CANNOT_BE_PRIMARY_KEY_EXCEPTION_CODE, newAttribute.getName()));

        if (!oldStructure.hasPrimary())
            throw new UserException(new Message(REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));

        Structure.Reference oldReference = oldStructure.getReference(newReference.getAttribute());
        if (!StructureUtils.isDisplayExpressionEquals(oldReference, newReference)) {
            validateReferenceAbility(newReference);
        }
    }

    /**
     * Проверка атрибута перед удалением из структуры.
     *
     * @param oldAttribute удаляемый атрибут
     * @param oldStructure исходная структура
     * @param refBookCode  код справочника
     */
    @Override
    public void validateOldAttribute(Structure.Attribute oldAttribute,
                                     Structure oldStructure, String refBookCode) {

        if (oldAttribute.hasIsPrimary() &&
                !isEmpty(oldStructure.getReferences()) &&
                oldStructure.getPrimaries().size() == 1)
            throw new UserException(new Message(REFERENCE_BOOK_MUST_HAVE_PRIMARY_KEY_EXCEPTION_CODE, refBookCode));
    }

    /**
     * Проверка первичных ключей на совпадение.
     *
     * @param primaries1 первичные ключи из первой структуры
     * @param primaries2 первичные ключи из второй структуры
     * @return Результат проверки
     */
    public boolean equalsPrimaries(List<Structure.Attribute> primaries1,
                                   List<Structure.Attribute> primaries2) {
        return !CollectionUtils.isEmpty(primaries1) &&
                !CollectionUtils.isEmpty(primaries2)
                && primaries1.size() == primaries2.size()
                && primaries1.stream().allMatch(primary1 -> containsPrimary(primaries2, primary1));
    }

    /** Проверка на наличие первичного ключа в списке первичных ключей. */
    private static boolean containsPrimary(List<Structure.Attribute> primaries, Structure.Attribute primary) {

        return primaries.stream().anyMatch(p -> p.storageEquals(primary));
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isValidCode(String code) {
        return !StringUtils.isEmpty(code) && CODE_PATTERN.matcher(code).matches();
    }
}

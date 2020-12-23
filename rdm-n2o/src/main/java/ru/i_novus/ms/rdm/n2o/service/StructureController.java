package ru.i_novus.ms.rdm.n2o.service;

import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.refbook.RefBook;
import ru.i_novus.ms.rdm.api.model.validation.*;
import ru.i_novus.ms.rdm.api.model.version.*;
import ru.i_novus.ms.rdm.api.rest.DraftRestService;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.api.service.RefBookService;
import ru.i_novus.ms.rdm.api.util.ConflictUtils;
import ru.i_novus.ms.rdm.api.util.StructureUtils;
import ru.i_novus.ms.rdm.api.util.TimeUtils;
import ru.i_novus.ms.rdm.n2o.model.AttributeCriteria;
import ru.i_novus.ms.rdm.n2o.model.FormAttribute;
import ru.i_novus.ms.rdm.n2o.model.ReadAttribute;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;

import java.util.*;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.containsIgnoreCase;
import static org.springframework.util.StringUtils.isEmpty;
import static ru.i_novus.ms.rdm.api.model.version.UpdateAttributeRequest.setUpdateValueIfExists;

@Controller
@SuppressWarnings("WeakerAccess")
public class StructureController {

    @Autowired
    private RefBookService refBookService;

    @Autowired
    private VersionRestService versionService;

    @Autowired
    private DraftRestService draftService;

    @Autowired
    private ConflictService conflictService;

    // used in: attribute.query.xml
    RestPage<ReadAttribute> getPage(AttributeCriteria criteria) {

        Integer versionId = criteria.getVersionId();
        RefBookVersion version = versionService.getById(versionId);
        if (version.hasEmptyStructure()) {
            return new RestPage<>(new ArrayList<>(), Pageable.unpaged(), 0);
        }

        if (criteria.getOptLockValue() != null) {
            version.setOptLockValue(criteria.getOptLockValue());
        }

        List<Structure.Attribute> attributes = version.getStructure().getAttributes();
        List<AttributeValidation> validations = draftService.getAttributeValidations(versionId, criteria.getCode());

        List<ReadAttribute> list;
        if (!isEmpty(attributes)) {
            list = toPageAttributes(attributes, criteria).stream()
                    .map(attribute -> toReadAttribute(attribute, version, validations))
                    .collect(toList());
        } else {
            list = new ArrayList<>();
        }

        return new RestPage<>(list, PageRequest.of(criteria.getPage(), criteria.getSize()), attributes.size());
    }

    // used in: attributeDefault.query.xml
    ReadAttribute getDefault(AttributeCriteria criteria) {

        Integer versionId = criteria.getVersionId();

        ReadAttribute readAttribute = new ReadAttribute();
        readAttribute.setVersionId(versionId);
        readAttribute.setOptLockValue(criteria.getOptLockValue());
        readAttribute.setCode(criteria.getCode());

        enrichByRefBook(versionId, readAttribute);

        return readAttribute;
    }

    /** Отбор атрибутов, отображаемых на текущей странице. */
    private List<Structure.Attribute> toPageAttributes(List<Structure.Attribute> attributes, AttributeCriteria criteria) {
        return attributes.stream()
                .filter(attribute -> isCriteriaAttribute(attribute, criteria))
                .skip((long) (criteria.getPage() - 1) * criteria.getSize())
                .limit(criteria.getSize())
                .collect(toList());
    }

    /** Проверка на соответствие атрибута критерию поиска. */
    private boolean isCriteriaAttribute(Structure.Attribute attribute, AttributeCriteria criteria) {

        return (isEmpty(criteria.getCode()) || criteria.getCode().equals(attribute.getCode()))
                && (isEmpty(criteria.getName()) || containsIgnoreCase(attribute.getName(), criteria.getName()));
    }

    /** Преобразование атрибута в атрибут для отображения на форме. */
    private ReadAttribute toReadAttribute(Structure.Attribute attribute, RefBookVersion version,
                                          List<AttributeValidation> validations) {

        Structure.Reference reference = attribute.isReferenceType()
                ? version.getStructure().getReference(attribute.getCode())
                : null;
        ReadAttribute readAttribute = getReadAttribute(attribute, reference);
        enrichAtribute(readAttribute, getValidations(validations, attribute.getCode()));

        readAttribute.setVersionId(version.getId());
        readAttribute.setOptLockValue(version.getOptLockValue());

        readAttribute.setIsReferrer(!CollectionUtils.isEmpty(version.getStructure().getReferences()));
        readAttribute.setCodeExpression(DisplayExpression.toPlaceholder(attribute.getCode()));

        if (reference != null) {
            enrichReference(readAttribute, reference);
        }

        enrichByRefBook(version.getId(), readAttribute);

        return readAttribute;
    }

    private List<AttributeValidation> getValidations(List<AttributeValidation> validations, String attribute) {

        if (CollectionUtils.isEmpty(validations))
            return Collections.emptyList();

        return validations.stream().filter(v -> Objects.equals(attribute, v.getAttribute())).collect(toList());
    }

    /** Заполнение атрибута для отображения с учётом его представления. */
    private void enrichAtribute(ReadAttribute attribute, List<AttributeValidation> validations) {

        for (AttributeValidation validation : validations) {
            switch (validation.getType()) {
                case REQUIRED:
                    attribute.setRequired(true);
                    break;

                case UNIQUE:
                    attribute.setUnique(true);
                    break;

                case PLAIN_SIZE:
                    attribute.setPlainSize(((PlainSizeAttributeValidation) validation).getSize());
                    break;

                case FLOAT_SIZE:
                    FloatSizeAttributeValidation floatSize = (FloatSizeAttributeValidation) validation;
                    attribute.setIntPartSize(floatSize.getIntPartSize());
                    attribute.setFracPartSize(floatSize.getFracPartSize());
                    break;

                case INT_RANGE:
                    IntRangeAttributeValidation intRange = (IntRangeAttributeValidation) validation;
                    attribute.setMinInteger(intRange.getMin());
                    attribute.setMaxInteger(intRange.getMax());
                    break;

                case FLOAT_RANGE:
                    FloatRangeAttributeValidation floatRange = (FloatRangeAttributeValidation) validation;
                    attribute.setMinFloat(floatRange.getMin());
                    attribute.setMaxFloat(floatRange.getMax());
                    break;

                case DATE_RANGE:
                    DateRangeAttributeValidation dateRange = (DateRangeAttributeValidation) validation;
                    attribute.setMinDate(dateRange.getMin());
                    attribute.setMaxDate(dateRange.getMax());
                    break;

                case REG_EXP:
                    attribute.setRegExp(((RegExpAttributeValidation) validation).getRegExp());
                    break;

                default:
                    break;
            }
        }
    }

    /** Заполнение атрибута-ссылки для отображения с учётом его представления. */
    private void enrichReference(ReadAttribute attribute, Structure.Reference reference) {

        Integer referenceRefBookId = refBookService.getId(reference.getReferenceCode());
        attribute.setReferenceRefBookId(referenceRefBookId);

        int displayType = 1;
        String displayExpression = reference.getDisplayExpression();
        if (StringUtils.isNotEmpty(displayExpression)) {
            attribute.setDisplayExpression(displayExpression);

            displayType = 2;
            String attributeCode = StructureUtils.displayExpressionToPlaceholder(displayExpression);
            if (attributeCode != null) {
                displayType = 1;
                attribute.setDisplayAttribute(attributeCode);
                attribute.setDisplayAttributeName(attributeCodeToName(reference.getReferenceCode(), attributeCode));
            }
        }
        attribute.setDisplayType(displayType);
    }

    private String attributeCodeToName(String refBookCode, String attributeCode) {

        RefBookVersion version = versionService.getLastPublishedVersion(refBookCode);
        Structure.Attribute attribute = version.getStructure().getAttribute(attributeCode);
        return (attribute != null) ? attribute.getName() : null;
    }

    /** Заполнение атрибута (+ ссылки) для отображения по информации о справочнике. */
    private void enrichByRefBook(Integer versionId, ReadAttribute readAttribute) {

        RefBook refBook = refBookService.getByVersionId(versionId);
        readAttribute.setHasReferrer(refBook.getHasReferrer());

        if (readAttribute.isReferenceType()) {
            readAttribute.setHasStructureConflict(getHasStructureConflict(refBook.getId(), readAttribute.getCode()));
        }
    }

    private Boolean getHasStructureConflict(Integer versionId, String fieldCode) {

        RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        conflictCriteria.setReferrerVersionId(versionId);
        conflictCriteria.setRefFieldCodes(List.of(fieldCode));
        conflictCriteria.setConflictTypes(ConflictUtils.getStructureConflictTypes());
        conflictCriteria.setIsLastPublishedVersion(true);
        conflictCriteria.setPageSize(1);

        Page<RefBookConflict> conflicts = conflictService.search(conflictCriteria);
        return conflicts != null && !CollectionUtils.isEmpty(conflicts.getContent());
    }

    public void createAttribute(Integer versionId, Integer optLockValue, FormAttribute formAttribute) {

        CreateAttributeRequest attributeRequest = getCreateAttributeRequest(optLockValue, formAttribute);
        draftService.createAttribute(versionId, attributeRequest);
        try {
            AttributeValidationRequest validationRequest = new AttributeValidationRequest();
            validationRequest.setNewAttribute(attributeRequest);
            validationRequest.setValidations(createValidations(formAttribute));

            draftService.updateAttributeValidations(versionId, validationRequest);

        } catch (RestException re) {
            DeleteAttributeRequest rollbackRequest = new DeleteAttributeRequest(null, formAttribute.getCode());
            draftService.deleteAttribute(versionId, rollbackRequest);
            throw re;
        }
    }

    public void updateAttribute(Integer versionId, Integer optLockValue, FormAttribute formAttribute) {

        Structure oldStructure = versionService.getStructure(versionId);
        Structure.Attribute oldAttribute = oldStructure.getAttribute(formAttribute.getCode());
        Structure.Reference oldReference = oldStructure.getReference(formAttribute.getCode());

        UpdateAttributeRequest attributeRequest = getUpdateAttributeRequest(optLockValue, formAttribute);
        draftService.updateAttribute(versionId, attributeRequest);
        try {
            AttributeValidationRequest validationRequest = new AttributeValidationRequest();
            validationRequest.setOldAttribute(getVersionAttribute(versionId, oldAttribute, oldReference));
            validationRequest.setNewAttribute(getCreateAttributeRequest(optLockValue, formAttribute));
            validationRequest.setValidations(createValidations(formAttribute));

            draftService.updateAttributeValidations(versionId, validationRequest);

        } catch (RestException re) {
            UpdateAttributeRequest rollbackRequest = new UpdateAttributeRequest(null, oldAttribute, oldReference);
            draftService.updateAttribute(versionId, rollbackRequest);
            throw re;
        }
    }

    public void deleteAttribute(Integer versionId, Integer optLockValue, String attributeCode) {

        draftService.deleteAttributeValidation(versionId, attributeCode, null);

        DeleteAttributeRequest request = new DeleteAttributeRequest(optLockValue, attributeCode);
        draftService.deleteAttribute(versionId, request);
    }

    /** Заполнение валидаций атрибута из атрибута формы. */
    private List<AttributeValidation> createValidations(FormAttribute formAttribute) {

        List<AttributeValidation> validations = new ArrayList<>();

        if (Boolean.TRUE.equals(formAttribute.getRequired())) {
            validations.add(new RequiredAttributeValidation());
        }
        if (Boolean.TRUE.equals(formAttribute.getUnique())) {
            validations.add(new UniqueAttributeValidation());
        }

        if (formAttribute.getPlainSize() != null) {
            validations.add(new PlainSizeAttributeValidation(formAttribute.getPlainSize()));
        }
        if (formAttribute.getIntPartSize() != null || formAttribute.getFracPartSize() != null) {
            validations.add(new FloatSizeAttributeValidation(formAttribute.getIntPartSize(), formAttribute.getFracPartSize()));
        }
        if (formAttribute.getMinInteger() != null || formAttribute.getMaxInteger() != null) {
            validations.add(new IntRangeAttributeValidation(formAttribute.getMinInteger(), formAttribute.getMaxInteger()));
        }
        if (formAttribute.getMinFloat() != null || formAttribute.getMaxFloat() != null) {
            validations.add(new FloatRangeAttributeValidation(formAttribute.getMinFloat(), formAttribute.getMaxFloat()));
        }

        if (formAttribute.getMinDate() != null || formAttribute.getMaxDate() != null) {
            validations.add(new DateRangeAttributeValidation(formAttribute.getMinDate(), formAttribute.getMaxDate()));
        }

        if (formAttribute.getRegExp() != null) {
            validations.add(new RegExpAttributeValidation(formAttribute.getRegExp()));
        }

        return validations;
    }

    /** Получение атрибута для отображения на форме из конкретного атрибута (+ ссылки). */
    private ReadAttribute getReadAttribute(Structure.Attribute attribute,
                                           Structure.Reference reference) {

        ReadAttribute readAttribute = new ReadAttribute();
        readAttribute.setCode(attribute.getCode());
        readAttribute.setName(attribute.getName());
        readAttribute.setType(attribute.getType());

        readAttribute.setIsPrimary(attribute.getIsPrimary());
        readAttribute.setLocalizable(attribute.getLocalizable());
        readAttribute.setDescription(attribute.getDescription());

        if (Objects.nonNull(reference)) {
            readAttribute.setReferenceCode(reference.getReferenceCode());
            readAttribute.setDisplayExpression(reference.getDisplayExpression());
        }

        return readAttribute;
    }

    /** Получение атрибута для версии из конкретного атрибута (+ ссылки). */
    private RefBookVersionAttribute getVersionAttribute(Integer versionId,
                                                        Structure.Attribute attribute,
                                                        Structure.Reference reference) {
        return new RefBookVersionAttribute(versionId, attribute, reference);
    }

    /** Получение атрибута для добавления из атрибута формы. */
    private CreateAttributeRequest getCreateAttributeRequest(Integer optLockValue,
                                                             FormAttribute formAttribute) {
        return new CreateAttributeRequest(optLockValue,
                buildAttribute(formAttribute), buildReference(formAttribute)
        );
    }

    private Structure.Attribute buildAttribute(FormAttribute request) {

        if (request.hasIsPrimary()) {
            return Structure.Attribute.buildPrimary(request.getCode(),
                    request.getName(), request.getType(), request.getDescription());

        } else if (request.isLocalizable()) {
            return Structure.Attribute.buildLocalizable(request.getCode(),
                    request.getName(), request.getType(), request.getDescription());

        } else {
            return Structure.Attribute.build(request.getCode(), request.getName(),
                    request.getType(), request.getDescription());
        }
    }

    private Structure.Reference buildReference(FormAttribute request) {

        return new Structure.Reference(request.getCode(),
                request.getReferenceCode(),
                request.getDisplayExpression());
    }

    /** Получение атрибута для изменения из атрибута формы. */
    private UpdateAttributeRequest getUpdateAttributeRequest(Integer optLockValue,
                                                             FormAttribute formAttribute) {

        UpdateAttributeRequest attribute = new UpdateAttributeRequest();
        attribute.setLastActionDate(TimeUtils.nowZoned());

        attribute.setOptLockValue(optLockValue);

        // attribute fields:
        attribute.setCode(formAttribute.getCode());
        attribute.setType(formAttribute.getType());
        attribute.setDescription(formAttribute.getDescription());

        setUpdateValueIfExists(formAttribute::getName, attribute::setName);
        setUpdateValueIfExists(formAttribute::getIsPrimary, attribute::setIsPrimary);
        setUpdateValueIfExists(formAttribute::getLocalizable, attribute::setLocalizable);

        // reference fields:
        setUpdateValueIfExists(formAttribute::getCode, attribute::setAttribute);
        setUpdateValueIfExists(formAttribute::getReferenceCode, attribute::setReferenceCode);
        setUpdateValueIfExists(formAttribute::getDisplayExpression, attribute::setDisplayExpression);

        return attribute;
    }
}

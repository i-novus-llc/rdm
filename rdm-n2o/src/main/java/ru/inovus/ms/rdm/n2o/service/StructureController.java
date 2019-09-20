package ru.inovus.ms.rdm.n2o.service;

import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.inovus.ms.rdm.api.model.validation.*;
import ru.inovus.ms.rdm.api.model.version.*;
import ru.inovus.ms.rdm.api.service.ConflictService;
import ru.inovus.ms.rdm.api.service.DraftService;
import ru.inovus.ms.rdm.api.service.RefBookService;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.n2o.model.AttributeCriteria;
import ru.inovus.ms.rdm.n2o.model.FormAttribute;
import ru.inovus.ms.rdm.n2o.model.ReadAttribute;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.inovus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.api.util.ConflictUtils;
import ru.inovus.ms.rdm.api.util.StructureUtils;
import ru.inovus.ms.rdm.api.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Controller
@SuppressWarnings("WeakerAccess")
public class StructureController {

    @Autowired
    private RefBookService refBookService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private DraftService draftService;
    @Autowired
    private ConflictService conflictService;

    RestPage<ReadAttribute> getPage(AttributeCriteria criteria) {
        List<ReadAttribute> list = new ArrayList<>();

        Structure structure = versionService.getStructure(criteria.getVersionId());

        List<AttributeValidation> attributeValidations = draftService.getAttributeValidations(criteria.getVersionId(), null);

        if (structure != null)
            structure.getAttributes().stream()
                    .filter(attribute -> isCriteriaAttribute(attribute, criteria))
                    .forEach(attribute -> {
                        Structure.Reference reference = attribute.isReferenceType() ? structure.getReference(attribute.getCode()) : null;
                        ReadAttribute readAttribute = getReadAttribute(attribute, reference);
                        enrichAtribute(readAttribute, filterByAttribute(attributeValidations, attribute.getCode()));

                        readAttribute.setVersionId(criteria.getVersionId());

                        RefBook refBook = refBookService.getByVersionId(criteria.getVersionId());
                        readAttribute.setHasReferrer(refBook.getHasReferrer());

                        readAttribute.setCodeExpression(DisplayExpression.toPlaceholder(attribute.getCode()));
                        if (reference != null) {
                            enrichReference(readAttribute, reference);

                            readAttribute.setHasStructureConflict(getHasStructureConflict(refBook.getId(), readAttribute.getCode()));
                        }

                        list.add(readAttribute);
                    });

        List<ReadAttribute> currentPageAttributes = list.stream()
                .skip((long) (criteria.getPage() - 1) * criteria.getSize())
                .limit(criteria.getSize())
                .collect(Collectors.toList());

        return new RestPage<>(currentPageAttributes, PageRequest.of(criteria.getPage(), criteria.getSize()), list.size());
    }

    private boolean isCriteriaAttribute(Structure.Attribute attribute, AttributeCriteria criteria) {
        return (Objects.isNull(criteria.getCode()) || criteria.getCode().equals(attribute.getCode()))
                && (Objects.isNull(criteria.getName()) || StringUtils.containsIgnoreCase(attribute.getName(), criteria.getName()));
    }

    private Boolean getHasStructureConflict(Integer versionId, String fieldCode) {

        RefBookConflictCriteria conflictCriteria = new RefBookConflictCriteria();
        conflictCriteria.setReferrerVersionId(versionId);
        conflictCriteria.setRefFieldCode(fieldCode);
        conflictCriteria.setConflictTypes(ConflictUtils.getStructureConflictTypes());
        conflictCriteria.setPageSize(1);

        Page<RefBookConflict> conflicts = conflictService.search(conflictCriteria);
        return conflicts != null && !CollectionUtils.isEmpty(conflicts.getContent());
    }

    public void createAttribute(Integer versionId, FormAttribute formAttribute) {

        CreateAttribute attributeModel = getCreateAttribute(versionId, formAttribute);
        draftService.createAttribute(attributeModel);
        try {
            AttributeValidationRequest validationRequest = new AttributeValidationRequest();
            validationRequest.setNewAttribute(attributeModel);
            validationRequest.setValidations(createValidations(formAttribute));

            draftService.updateAttributeValidations(versionId, validationRequest);

        } catch (RestException re) {
            draftService.deleteAttribute(versionId, formAttribute.getCode());
            throw re;
        }
    }

    public void updateAttribute(Integer versionId, FormAttribute formAttribute) {

        Structure oldStructure = versionService.getStructure(versionId);
        Structure.Attribute oldAttribute = oldStructure.getAttribute(formAttribute.getCode());
        Structure.Reference oldReference = oldStructure.getReference(formAttribute.getCode());

        UpdateAttribute attributeModel = getUpdateAttribute(versionId, formAttribute);
        draftService.updateAttribute(attributeModel);
        try {
            AttributeValidationRequest validationRequest = new AttributeValidationRequest();
            validationRequest.setOldAttribute(getVersionAttribute(versionId, oldAttribute, oldReference));
            validationRequest.setNewAttribute(getCreateAttribute(versionId, formAttribute));
            validationRequest.setValidations(createValidations(formAttribute));

            draftService.updateAttributeValidations(versionId, validationRequest);

        } catch (RestException re) {
            draftService.updateAttribute(new UpdateAttribute(versionId, oldAttribute, oldReference));
            throw re;
        }
    }

    public void deleteAttribute(Integer versionId, String attributeCode) {

        draftService.deleteAttributeValidation(versionId, attributeCode, null);
        draftService.deleteAttribute(versionId, attributeCode);
    }

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

    private String attributeCodeToName(String refBookCode, String attributeCode) {

        RefBookVersion version = versionService.getLastPublishedVersion(refBookCode);
        Structure.Attribute attribute = version.getStructure().getAttribute(attributeCode);
        return (attribute != null) ? attribute.getName() : null;
    }

    private void enrichReference(ReadAttribute attribute, Structure.Reference reference) {

        Integer refRefBookId = refBookService.getId(reference.getReferenceCode());
        attribute.setReferenceRefBookId(refRefBookId);

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

    private List<AttributeValidation> filterByAttribute(List<AttributeValidation> validations, String attribute) {
        return validations.stream()
                .filter(v -> Objects.equals(attribute, v.getAttribute()))
                .collect(Collectors.toList());
    }

    private ReadAttribute getReadAttribute(Structure.Attribute structureAttribute, Structure.Reference reference) {

        ReadAttribute attribute = new ReadAttribute();
        attribute.setCode(structureAttribute.getCode());
        attribute.setName(structureAttribute.getName());
        attribute.setDescription(structureAttribute.getDescription());
        attribute.setType(structureAttribute.getType());
        attribute.setIsPrimary(structureAttribute.getIsPrimary());

        if (Objects.nonNull(reference)) {
            attribute.setReferenceCode(reference.getReferenceCode());
            attribute.setDisplayExpression(reference.getDisplayExpression());
        }

        return attribute;
    }

    private RefBookVersionAttribute getVersionAttribute(Integer versionId, Structure.Attribute attribute, Structure.Reference reference) {
        return new RefBookVersionAttribute(versionId, attribute, reference);
    }

    private CreateAttribute getCreateAttribute(Integer versionId, FormAttribute formAttribute) {
        return new CreateAttribute(versionId, buildAttribute(formAttribute), buildReference(formAttribute));
    }

    private Structure.Attribute buildAttribute(FormAttribute request) {
        if (request.getIsPrimary())
            return Structure.Attribute.buildPrimary(request.getCode(),
                    request.getName(), request.getType(), request.getDescription());
        else {
            return Structure.Attribute.build(request.getCode(), request.getName(),
                    request.getType(), request.getDescription());
        }
    }

    private Structure.Reference buildReference(FormAttribute request) {
        return new Structure.Reference(request.getCode(),
                request.getReferenceCode(),
                request.getDisplayExpression());
    }

    private UpdateAttribute getUpdateAttribute(Integer versionId, FormAttribute formAttribute) {

        UpdateAttribute attribute = new UpdateAttribute();
        attribute.setLastActionDate(TimeUtils.nowZoned());
        attribute.setVersionId(versionId);

        attribute.setCode(formAttribute.getCode());

        if (formAttribute.getName() != null)
            attribute.setName(UpdateValue.of(formAttribute.getName()));
        attribute.setType(formAttribute.getType());
        if (formAttribute.getIsPrimary() != null)
            attribute.setIsPrimary(UpdateValue.of(formAttribute.getIsPrimary()));
        if (formAttribute.getDescription() != null)
            attribute.setDescription(UpdateValue.of(formAttribute.getDescription()));
        attribute.setAttribute(UpdateValue.of(formAttribute.getCode()));
        if (formAttribute.getReferenceCode() != null)
            attribute.setReferenceCode(UpdateValue.of(formAttribute.getReferenceCode()));
        if (formAttribute.getDisplayExpression() != null)
            attribute.setDisplayExpression(UpdateValue.of(formAttribute.getDisplayExpression()));

        return attribute;
    }
}

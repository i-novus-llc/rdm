package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DisplayExpression;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.version.CreateAttribute;
import ru.inovus.ms.rdm.model.version.UpdateAttribute;
import ru.inovus.ms.rdm.model.validation.*;
import ru.inovus.ms.rdm.model.version.RefBookVersion;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.model.version.UpdateValue.of;

@Controller
@SuppressWarnings("WeakerAccess")
public class StructureController {

    @Autowired
    private RefBookService refBookService;
    @Autowired
    private VersionService versionService;
    @Autowired
    private DraftService draftService;

    RestPage<ReadAttribute> getPage(AttributeCriteria criteria) {
        List<ReadAttribute> list = new ArrayList<>();

        Structure structure = versionService.getStructure(criteria.getVersionId());

        List<AttributeValidation> attributeValidations = draftService.getAttributeValidations(criteria.getVersionId(), null);

        if (structure != null)
            structure.getAttributes().forEach(attribute -> {
                if (Objects.isNull(criteria.getCode()) || criteria.getCode().equals(attribute.getCode())) {
                    Structure.Reference reference = !FieldType.REFERENCE.equals(attribute.getType()) ? null
                            : structure.getReference(attribute.getCode());
                    ReadAttribute readAttribute = model(attribute, reference);
                    enrich(readAttribute, filterByAttribute(attributeValidations, attribute.getCode()));

                    readAttribute.setVersionId(criteria.getVersionId());
                    readAttribute.setCodeExpression(DisplayExpression.toPlaceholder(attribute.getCode()));
                    if (reference != null)
                        enrich(readAttribute, reference);
                    list.add(readAttribute);
                }
            });

        List<ReadAttribute> currentPageAttributes = list.stream()
                .skip((long) (criteria.getPage() - 1) * criteria.getSize())
                .limit(criteria.getSize())
                .collect(Collectors.toList());

        return new RestPage<>(currentPageAttributes, PageRequest.of(criteria.getPage(), criteria.getSize()), list.size());
    }

    public void createAttribute(Integer versionId, FormAttribute formAttribute) {

        CreateAttribute attributeModel = new CreateAttribute();
        attributeModel.setVersionId(versionId);
        attributeModel.setAttribute(buildAttribute(formAttribute));
        attributeModel.setReference(buildReference(formAttribute));

        draftService.createAttribute(attributeModel);
        try {
            draftService.updateAttributeValidations(versionId, formAttribute.getCode(), createValidations(formAttribute));

        } catch (RestException re) {
            draftService.deleteAttribute(versionId, formAttribute.getCode());
            throw re;
        }
    }

    public void updateAttribute(Integer versionId, FormAttribute formAttribute) {

        Structure oldStructure = versionService.getStructure(versionId);
        Structure.Attribute oldAttribute = oldStructure.getAttribute(formAttribute.getCode());
        Structure.Reference oldReference = oldStructure.getReference(formAttribute.getCode());

        draftService.updateAttribute(getUpdateAttribute(versionId, formAttribute));
        try {
            draftService.updateAttributeValidations(versionId, formAttribute.getCode(), createValidations(formAttribute));

        } catch (RestException re) {
            draftService.updateAttribute(new UpdateAttribute(versionId, oldAttribute, oldReference));
            throw re;
        }
    }

    public void deleteAttribute(Integer versionId, String attributeCode) {

        draftService.deleteAttribute(versionId, attributeCode);
        draftService.deleteAttributeValidation(versionId, attributeCode, null);
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

    private String displayExpressionToAttributeCode(String displayExpression) {

        DisplayExpression expression = new DisplayExpression(displayExpression);
        List<String> attributeCodes = expression.getPlaceholders();
        if (attributeCodes != null && attributeCodes.size() == 1) {
            String attributeCode = attributeCodes.get(0);
            if (DisplayExpression.toPlaceholder(attributeCode).equals(displayExpression)) {
                return attributeCode;
            }
        }
        return null;
    }

    private String attributeCodeToName(String refBookCode, String attributeCode) {

        RefBookVersion version = versionService.getLastPublishedVersion(refBookCode);
        Structure.Attribute attribute = version.getStructure().getAttribute(attributeCode);
        return (attribute != null) ? attribute.getName() : null;
    }

    private void enrich(ReadAttribute attribute, Structure.Reference reference) {

        Integer refRefBookId = refBookService.getId(reference.getReferenceCode());
        attribute.setReferenceRefBookId(refRefBookId);

        int displayType = 1;
        String displayExpression = reference.getDisplayExpression();
        if (StringUtils.isNotEmpty(displayExpression)) {
            attribute.setDisplayExpression(displayExpression);

            displayType = 2;
            String attributeCode = displayExpressionToAttributeCode(displayExpression);
            if (attributeCode != null) {
                displayType = 1;
                attribute.setDisplayAttribute(attributeCode);
                attribute.setDisplayAttributeName(attributeCodeToName(reference.getReferenceCode(), attributeCode));
            }
        }
        attribute.setDisplayType(displayType);
    }

    private void enrich(ReadAttribute attribute, List<AttributeValidation> validations) {
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

        UpdateAttribute updateAttribute = new UpdateAttribute();
        updateAttribute.setLastActionDate(TimeUtils.nowZoned());
        updateAttribute.setVersionId(versionId);
        updateAttribute.setCode(formAttribute.getCode());

        if (formAttribute.getName() != null)
            updateAttribute.setName(of(formAttribute.getName()));
        updateAttribute.setType(formAttribute.getType());
        if (formAttribute.getIsPrimary() != null)
            updateAttribute.setIsPrimary(of(formAttribute.getIsPrimary()));
        if (formAttribute.getDescription() != null)
            updateAttribute.setDescription(of(formAttribute.getDescription()));
        updateAttribute.setAttribute(of(formAttribute.getCode()));
        if (formAttribute.getReferenceCode() != null)
            updateAttribute.setReferenceCode(of(formAttribute.getReferenceCode()));
        if (formAttribute.getDisplayExpression() != null)
            updateAttribute.setDisplayExpression(of(formAttribute.getDisplayExpression()));

        return updateAttribute;
    }

    private ReadAttribute model(Structure.Attribute structureAttribute, Structure.Reference reference) {

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
}

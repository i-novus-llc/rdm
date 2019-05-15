package ru.inovus.ms.rdm.service;

import net.n2oapp.platform.jaxrs.RestException;
import net.n2oapp.platform.jaxrs.RestPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.model.validation.*;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.model.UpdateValue.of;

@Controller
public class StructureController {

    @Autowired
    private VersionService versionService;
    @Autowired
    private DraftService draftService;
    @Autowired
    private RefBookService refBookService;

    public RestPage<ReadAttribute> getPage(AttributeCriteria criteria) {
        List<ReadAttribute> list = new ArrayList<>();

        Structure structure = versionService.getStructure(criteria.getVersionId());

        List<AttributeValidation> attributeValidations = draftService.getAttributeValidations(criteria.getVersionId(), null);

        if (structure != null)
            structure.getAttributes().forEach(a -> {
                if (Objects.isNull(criteria.getCode()) || criteria.getCode().equals(a.getCode())) {
                    Structure.Reference reference = !FieldType.REFERENCE.equals(a.getType()) ? null
                            : structure.getReference(a.getCode());
                    ReadAttribute attribute = model(a, reference);
                    enrich(attribute, filterByAttribute(attributeValidations, a.getCode()));
                    attribute.setVersionId(criteria.getVersionId());
                    if (reference != null)
                        enrich(attribute, reference);
                    list.add(attribute);
                }
            });
        List<ReadAttribute> currentPageAttributes = list.stream()
                .skip((long) (criteria.getPage() - 1) * criteria.getSize())
                .limit(criteria.getSize())
                .collect(Collectors.toList());

        return new RestPage<>(currentPageAttributes, new PageRequest(criteria.getPage(), criteria.getSize()), list.size());
    }

    public void createAttribute(Integer versionId, Attribute attribute) {
        CreateAttribute attributeModel = new CreateAttribute();
        attributeModel.setVersionId(versionId);
        attributeModel.setAttribute(buildAttribute(attribute));
        attributeModel.setReference(buildReference(attribute));
        draftService.createAttribute(attributeModel);
        try {
            draftService.updateAttributeValidations(versionId, attribute.getCode(), createValidations(attribute));
        } catch (RestException re) {
            draftService.deleteAttribute(versionId, attribute.getCode());
            throw re;
        }
    }

    public void updateAttribute(Integer versionId, Attribute attribute) {
        Structure.Attribute oldAttribute = versionService.getStructure(versionId).getAttribute(attribute.getCode());
        Structure.Reference oldReference = versionService.getStructure(versionId).getReference(attribute.getCode());
        draftService.updateAttribute(getUpdateAttribute(versionId, attribute));
        try {
            draftService.updateAttributeValidations(versionId, attribute.getCode(), createValidations(attribute));
        } catch (RestException re) {
            draftService.updateAttribute(new UpdateAttribute(versionId, oldAttribute, oldReference));
            throw re;
        }
    }

    public void deleteAttribute(Integer versionId, Attribute attribute) {
        draftService.deleteAttribute(versionId, attribute.getCode());
        draftService.deleteAttributeValidation(versionId, attribute.getCode(), null);
    }

    private List<AttributeValidation> createValidations(Attribute attribute) {
        List<AttributeValidation> validations = new ArrayList<>();
        if (Boolean.TRUE.equals(attribute.getRequired()))
            validations.add(new RequiredAttributeValidation());
        if (Boolean.TRUE.equals(attribute.getUnique()))
            validations.add(new UniqueAttributeValidation());
        if (attribute.getPlainSize() != null)
            validations.add(new PlainSizeAttributeValidation(attribute.getPlainSize()));
        if (attribute.getIntPartSize() != null || attribute.getFracPartSize() != null) {
            validations.add(new FloatSizeAttributeValidation(attribute.getIntPartSize(), attribute.getFracPartSize()));
        }
        if (attribute.getMinInteger() != null || attribute.getMaxInteger() != null) {
            validations.add(new IntRangeAttributeValidation(attribute.getMinInteger(), attribute.getMaxInteger()));
        }
        if (attribute.getMinFloat() != null || attribute.getMaxFloat() != null) {
            validations.add(new FloatRangeAttributeValidation(attribute.getMinFloat(), attribute.getMaxFloat()));
        }
        if (attribute.getMinDate() != null || attribute.getMaxDate() != null) {
            validations.add(new DateRangeAttributeValidation(attribute.getMinDate(), attribute.getMaxDate()));
        }
        if (attribute.getRegExp() != null) {
            validations.add(new RegExpAttributeValidation(attribute.getRegExp()));
        }
        return validations;
    }

    private void enrich(ReadAttribute attribute, Structure.Reference reference) {
        Integer refRefBookId = refBookService.getId(reference.getReferenceCode());
        attribute.setReferenceRefBookId(refRefBookId);

        RefBookVersion version = versionService.getLastPublishedVersion(attribute.getReferenceCode());
        String attributeName = reference.findReferenceAttribute(version.getStructure()).getName();
        attribute.setReferenceAttributeName(attributeName);
        attribute.setReferenceDisplayExpression(reference.getDisplayExpression());
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
        return validations.stream().filter(v -> Objects.equals(attribute, v.getAttribute())).collect(Collectors.toList());
    }

    private Structure.Attribute buildAttribute(Attribute request) {
        if (request.getIsPrimary())
            return Structure.Attribute.buildPrimary(request.getCode(),
                    request.getName(), request.getType(), request.getDescription());
        else {
            return Structure.Attribute.build(request.getCode(), request.getName(),
                    request.getType(), request.getDescription());
        }
    }

    private Structure.Reference buildReference(Attribute request) {
        return new Structure.Reference(request.getCode(),
                request.getReferenceCode(), request.getReferenceAttribute(),
                request.getReferenceDisplayExpression());
    }

    private UpdateAttribute getUpdateAttribute(Integer versionId, Attribute attribute) {
        UpdateAttribute updateAttribute = new UpdateAttribute();
        updateAttribute.setLastActionDate(TimeUtils.nowZoned());
        updateAttribute.setVersionId(versionId);
        updateAttribute.setCode(attribute.getCode());
        if (attribute.getName() != null)
            updateAttribute.setName(of(attribute.getName()));
        updateAttribute.setType(attribute.getType());
        if (attribute.getIsPrimary() != null)
            updateAttribute.setIsPrimary(of(attribute.getIsPrimary()));
        if (attribute.getDescription() != null)
            updateAttribute.setDescription(of(attribute.getDescription()));
        updateAttribute.setAttribute(of(attribute.getCode()));
        if (attribute.getReferenceCode() != null)
            updateAttribute.setReferenceCode(of(attribute.getReferenceCode()));
        if (attribute.getReferenceAttribute() != null)
            updateAttribute.setReferenceAttribute(of(attribute.getReferenceAttribute()));
        if (attribute.getReferenceDisplayExpression() != null)
            updateAttribute.setDisplayExpression(of(attribute.getReferenceDisplayExpression()));
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
            attribute.setReferenceAttribute(reference.getReferenceAttribute());
            attribute.setReferenceDisplayExpression(reference.getDisplayExpression());
        }
        return attribute;
    }
}

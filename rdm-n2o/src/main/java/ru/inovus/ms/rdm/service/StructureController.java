package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.CollectionPageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.*;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.model.UpdateValue.of;

@Controller
public class StructureController implements CollectionPageService<AttributeCriteria, ReadAttribute> {

    @Autowired
    private VersionService versionService;
    @Autowired
    private DraftService draftService;
    @Autowired
    private RefBookService refBookService;

    @Override
    public CollectionPage<ReadAttribute> getCollectionPage(AttributeCriteria criteria) {
        List<ReadAttribute> list = new ArrayList<>();

        Structure structure = versionService.getStructure(criteria.getVersionId());
        if (structure != null)
            structure.getAttributes().forEach(a -> {
                if (Objects.isNull(criteria.getCode()) || criteria.getCode().equals(a.getCode())) {
                    Structure.Reference reference = !FieldType.REFERENCE.equals(a.getType()) ? null
                            : structure.getReference(a.getCode());
                    ReadAttribute attribute = model(a, reference);
                    attribute.setVersionId(criteria.getVersionId());
                    if (reference != null)
                        enrich(attribute, reference);
                    list.add(attribute);
                }
            });
        // noinspection unchecked
        return new CollectionPage(list.size(), list, criteria);
    }

    public void createAttribute(Integer versionId, Attribute attribute) {
        CreateAttribute attributeModel = new CreateAttribute();
        attributeModel.setVersionId(versionId);
        attributeModel.setAttribute(buildAttribute(attribute));
        attributeModel.setReference(buildReference(attribute));
        draftService.createAttribute(attributeModel);
    }

    public void updateAttribute(Integer versionId, Attribute attribute) {
        draftService.updateAttribute(getUpdateAttribute(versionId, attribute));
    }

    private void enrich(ReadAttribute attribute, Structure.Reference reference) {
        Integer refRefBookId = refBookService.getById(reference.getReferenceVersion()).getRefBookId();
        attribute.setReferenceRefBookId(refRefBookId);

        String attributeName = getAttributeName(reference.getReferenceAttribute(), attribute.getReferenceVersion());
        attribute.setReferenceAttributeName(attributeName);

        List<String> displayAttributes = reference.getDisplayAttributes();
        if (!isEmpty(displayAttributes)) {
            String referenceDisplayAttributeName =
                    getAttributeName(reference.getDisplayAttributes().get(0), attribute.getReferenceVersion());
            attribute.setReferenceDisplayAttributeName(referenceDisplayAttributeName);
        }
    }

    private List<String> getReferenceDisplayAttributes(Attribute attribute) {
        if (!FieldType.REFERENCE.equals(attribute.getType()))
            return emptyList();
        return singletonList(StringUtils.isBlank(attribute.getReferenceDisplayAttribute()) ?
                attribute.getReferenceAttribute() : attribute.getReferenceDisplayAttribute());
    }

    private String getAttributeName(String attributeCode, Integer versionId) {
        return versionService.getStructure(versionId).getAttribute(attributeCode).getName();
    }

    private Structure.Attribute buildAttribute(Attribute request) {
        if (request.getIsPrimary())
            return Structure.Attribute.buildPrimary(request.getCode(),
                    request.getName(), request.getType(), request.getDescription());
        else {
            return Structure.Attribute.build(request.getCode(),
                    request.getName(), request.getType(),
                    request.getIsRequired(), request.getDescription());
        }
    }
    private Structure.Reference buildReference(Attribute request) {
        return new Structure.Reference(request.getCode(),
                request.getReferenceVersion(), request.getReferenceAttribute(),
                getReferenceDisplayAttributes(request), emptyList());
    }

    private UpdateAttribute getUpdateAttribute(Integer versionId, Attribute attribute) {
        UpdateAttribute updateAttribute = new UpdateAttribute();
        updateAttribute.setLastActionDate(LocalDateTime.of(LocalDate.now(), LocalTime.now()));
        updateAttribute.setVersionId(versionId);
        updateAttribute.setCode(attribute.getCode());
        if (attribute.getName() != null)
            updateAttribute.setName(of(attribute.getName()));
        updateAttribute.setType(attribute.getType());
        if (attribute.getIsRequired() != null)
            updateAttribute.setIsRequired(of(attribute.getIsRequired()));
        if (attribute.getIsPrimary() != null)
            updateAttribute.setIsPrimary(of(attribute.getIsPrimary()));
        if (attribute.getDescription() != null)
            updateAttribute.setDescription(of(attribute.getDescription()));
        updateAttribute.setAttribute(of(attribute.getCode()));
        if (attribute.getReferenceVersion() != null)
            updateAttribute.setReferenceVersion(of(attribute.getReferenceVersion()));
        if (attribute.getReferenceAttribute() != null)
            updateAttribute.setReferenceAttribute(of(attribute.getReferenceAttribute()));
        if (attribute.getReferenceDisplayAttribute() != null)
            updateAttribute.setDisplayAttributes(of(getReferenceDisplayAttributes(attribute)));
        return updateAttribute;
    }

    private ReadAttribute model(Structure.Attribute structureAttribute, Structure.Reference reference) {
        ReadAttribute attribute = new ReadAttribute();
        attribute.setCode(structureAttribute.getCode());
        attribute.setName(structureAttribute.getName());
        attribute.setDescription(structureAttribute.getDescription());
        attribute.setType(structureAttribute.getType());
        attribute.setIsRequired(structureAttribute.getIsRequired());
        attribute.setIsPrimary(structureAttribute.getIsPrimary());
        if (Objects.nonNull(reference)) {
            attribute.setReferenceVersion(reference.getReferenceVersion());
            attribute.setReferenceAttribute(reference.getReferenceAttribute());
            List<String> displayAttributes = reference.getDisplayAttributes();
            attribute.setReferenceDisplayAttribute(isEmpty(displayAttributes) ? null : displayAttributes.get(0));
        }
        return attribute;
    }
}

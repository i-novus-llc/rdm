package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.criteria.api.CollectionPageService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.model.Attribute;
import ru.inovus.ms.rdm.model.AttributeCriteria;
import ru.inovus.ms.rdm.model.ReadAttribute;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.RefBookService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

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
        Structure.Attribute structureAttribute = buildAttribute(attribute);
        draftService.createAttribute(versionId, structureAttribute,
                attribute.getReferenceVersion(),
                attribute.getReferenceAttribute(),
                getReferenceDisplayAttributes(attribute));
    }

    public void updateAttribute(Integer versionId, Attribute attribute) {
        Structure.Attribute structureAttribute = buildAttribute(attribute);
        draftService.updateAttribute(versionId, structureAttribute,
                attribute.getReferenceVersion(),
                attribute.getReferenceAttribute(),
                getReferenceDisplayAttributes(attribute));
    }

    private void enrich(ReadAttribute attribute, Structure.Reference reference) {
        Integer refRefBookId = refBookService.getById(reference.getReferenceVersion()).getRefBookId();
        attribute.setReferenceRefBookId(refRefBookId);

        String attributeName = getAttributeName(reference.getReferenceAttribute(), attribute.getReferenceVersion());
        attribute.setReferenceAttributeName(attributeName);

        List<String> displayAttributes = reference.getDisplayAttributes();
        if (!CollectionUtils.isEmpty(displayAttributes)) {
            String referenceDisplayAttributeName =
                    getAttributeName(reference.getDisplayAttributes().get(0), attribute.getReferenceVersion());
            attribute.setReferenceDisplayAttributeName(referenceDisplayAttributeName);
        }
    }

    private List<String> getReferenceDisplayAttributes(Attribute attribute) {
        if (!FieldType.REFERENCE.equals(attribute.getType()))
            return Collections.emptyList();
        return Collections.singletonList(StringUtils.isBlank(attribute.getReferenceDisplayAttribute()) ?
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
            attribute.setReferenceDisplayAttribute(CollectionUtils.isEmpty(displayAttributes) ? null : displayAttributes.get(0));
        }
        return attribute;
    }
}

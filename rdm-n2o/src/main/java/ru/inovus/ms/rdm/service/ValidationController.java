package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Objects;

@Service
public class ValidationController {

    @Autowired
    private VersionService versionService;

    public boolean checkAttributeCodeNotExists(Integer versionId, String attributeCode) {
        Structure structure = versionService.getStructure(versionId);
        return Objects.isNull(structure) || Objects.isNull(structure.getAttribute(attributeCode));
    }

    public boolean checkAttributeDeletable(Integer versionId) {
        Structure structure = versionService.getStructure(versionId);
        return Objects.isNull(structure) || Objects.isNull(structure.getAttributes()) || structure.getAttributes().size() > 1;
    }

}

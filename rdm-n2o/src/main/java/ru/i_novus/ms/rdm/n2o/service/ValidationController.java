package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.model.Structure;

import java.util.Objects;

@Service
@SuppressWarnings("unused")
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

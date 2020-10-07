package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;

@Service
@SuppressWarnings("unused")
public class ValidationController {

    @Autowired
    private VersionRestService versionService;

    public boolean checkAttributeCodeNotExists(Integer versionId, String attributeCode) {

        Structure structure = versionService.getStructure(versionId);
        return structure == null || structure.getAttribute(attributeCode) == null;
    }

    public boolean checkAttributeDeletable(Integer versionId) {

        Structure structure = versionService.getStructure(versionId);
        return structure == null || structure.getAttributes() == null || structure.getAttributes().size() > 1;
    }

}

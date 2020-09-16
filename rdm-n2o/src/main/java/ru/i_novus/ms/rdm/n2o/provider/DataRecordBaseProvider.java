package ru.i_novus.ms.rdm.n2o.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;

/**
 * Провайдер для формирования метаданных.
 */
@Service
public class DataRecordBaseProvider {

    private static final Logger logger = LoggerFactory.getLogger(DataRecordBaseProvider.class);

    protected VersionRestService versionService;

    @Autowired
    public void setVersionService(VersionRestService versionService) {
        this.versionService = versionService;
    }

    protected Structure getStructureOrNull(Integer versionId) {
        try {
            return versionService.getStructure(versionId);

        } catch (Exception e) {
            logger.error("Structure is not received for metadata", e);

            return null;
        }
    }

    protected boolean isEmptyStructure(Structure structure) {
        return structure == null || structure.isEmpty();
    }
}

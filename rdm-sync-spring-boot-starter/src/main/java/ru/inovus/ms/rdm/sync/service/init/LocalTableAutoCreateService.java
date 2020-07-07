package ru.inovus.ms.rdm.sync.service.init;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refbook.RefBook;
import ru.inovus.ms.rdm.sync.model.DataTypeEnum;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingField;
import ru.inovus.ms.rdm.sync.model.loader.XmlMappingRefBook;
import ru.inovus.ms.rdm.sync.rest.RdmSyncRest;
import ru.inovus.ms.rdm.sync.service.RdmSyncDao;

import java.util.ArrayList;
import java.util.List;

@Component
class LocalTableAutoCreateService {

    private static final Logger logger = LoggerFactory.getLogger(LocalTableAutoCreateService.class);

    @Autowired
    private RdmSyncDao dao;

    @Autowired
    private RdmSyncRest rdmSyncRest;

    @Transactional
    public void autoCreate(String refBookCode, String autoCreateSchema) {
        if (dao.getVersionMapping(refBookCode) != null) {
            logger.info("Skipping auto creation of structures of RefBook with code {}.", refBookCode);
            return;
        }
        logger.info("Auto creating structures of RefBook with code {}", refBookCode);
        RefBook lastPublished;
        try {
            lastPublished = rdmSyncRest.getLastPublishedVersionFromRdm(refBookCode);
        } catch (Exception e) {
            logger.error("Error while auto creating structures of RefBook with code {}. Can't get last published version from RDM.", refBookCode, e);
            return;
        }
        Structure structure = lastPublished.getStructure();
        String isDeletedField = "is_deleted";
        boolean isDeletedReserved = structure.getAttributes().stream().anyMatch(attribute -> "is_deleted".equals(attribute.getCode()));
        if (isDeletedReserved)
            isDeletedField = "rdm_sync_internal_" + isDeletedField;
        Structure.Attribute uniqueSysField = structure.getPrimary().get(0);
        XmlMappingRefBook mapping = new XmlMappingRefBook();
        mapping.setCode(refBookCode);
        mapping.setSysTable(String.format("%s.%s", autoCreateSchema, refBookCode.replaceAll("[-.]", "_").toLowerCase()));
        mapping.setDeletedField(isDeletedField);
        mapping.setUniqueSysField(uniqueSysField.getCode());
        mapping.setMappingVersion(-1);
        List<XmlMappingField> fields = new ArrayList<>(structure.getAttributes().size() + 1);
        for (Structure.Attribute attr : structure.getAttributes()) {
            XmlMappingField field = new XmlMappingField();
            field.setRdmField(attr.getCode());
            field.setSysField(attr.getCode());
            field.setSysDataType(DataTypeEnum.getByRdmAttr(attr).getDataTypes().get(0));
            fields.add(field);
        }
        dao.upsertVersionMapping(mapping);
        dao.insertFieldMapping(refBookCode, fields);
        logger.info("Structures of RefBook with code {} auto created.", refBookCode);
    }

}

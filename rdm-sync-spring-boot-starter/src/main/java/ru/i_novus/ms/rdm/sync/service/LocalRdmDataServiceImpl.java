package ru.i_novus.ms.rdm.sync.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.exception.RdmException;
import ru.i_novus.ms.rdm.sync.model.DataTypeEnum;
import ru.i_novus.ms.rdm.sync.model.FieldMapping;
import ru.i_novus.ms.rdm.sync.model.VersionMapping;
import ru.i_novus.ms.rdm.sync.rest.LocalRdmDataService;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Map;

import static ru.i_novus.ms.rdm.sync.service.RdmSyncLocalRowState.SYNCED;

@Service
public class LocalRdmDataServiceImpl implements LocalRdmDataService {

    @Autowired
    private RdmSyncDao dao;

    @Override
    public Page<Map<String, Object>> getData(String refBookCode, Boolean getDeleted, Integer page, Integer size, @Context UriInfo uriInfo) {
        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode);
        if (getDeleted == null) getDeleted = false;
        if (page == null) page = 0;
        if (size == null) size = 10;
        MultivaluedMap<String, Object> filters = filtersToObjects(dao.getFieldMapping(refBookCode), uriInfo.getQueryParameters());
        filters.putSingle(versionMapping.getDeletedField(), getDeleted);
        return dao.getData(versionMapping.getTable(), versionMapping.getPrimaryField(), size, page * size, SYNCED, filters);
    }

    @Override
    public Map<String, Object> getSingle(String refBookCode, String pk) {
        VersionMapping versionMapping = getVersionMappingOrThrowRefBookNotFound(refBookCode);
        FieldMapping fieldMapping = dao.getFieldMapping(refBookCode).stream().filter(fm -> fm.getSysField().equals(versionMapping.getPrimaryField())).findFirst().orElseThrow(() -> new RdmException(versionMapping.getPrimaryField() + " not found in RefBook with code " + refBookCode));
        DataTypeEnum dt = DataTypeEnum.getByDataType(fieldMapping.getSysDataType());
        Page<Map<String, Object>> synced = dao.getData(versionMapping.getTable(), versionMapping.getPrimaryField(), 1, 0, SYNCED, new MultivaluedHashMap<>(Map.of(versionMapping.getPrimaryField(), dt.castFromString(pk))));
        return synced.get().findAny().orElseThrow(NotFoundException::new);
    }

    private MultivaluedMap<String, Object> filtersToObjects(List<FieldMapping> fieldMapping, MultivaluedMap<String, String> filters) {
        MultivaluedMap<String, Object> res = new MultivaluedHashMap<>();
        for (MultivaluedMap.Entry<String, List<String>> e : filters.entrySet()) {
            fieldMapping.stream().filter(fm -> fm.getSysField().equals(e.getKey())).findAny().ifPresent(fm -> {
                DataTypeEnum dt = DataTypeEnum.getByDataType(fm.getSysDataType());
                if (dt != null) {
                    res.put(e.getKey(), (List<Object>) dt.castFromString(e.getValue()));
                }
            });
        }
        return res;
    }

    private VersionMapping getVersionMappingOrThrowRefBookNotFound(String refBookCode) {
        VersionMapping versionMapping = dao.getVersionMapping(refBookCode);
        if (versionMapping == null)
            throw new RdmException("RefBook with code " + refBookCode + " is not maintained in system.");
        return versionMapping;
    }

}

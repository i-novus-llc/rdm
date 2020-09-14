package ru.i_novus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataCriteria;
import ru.i_novus.ms.rdm.n2o.api.service.RefBookDataService;

import java.util.List;

@Service
@SuppressWarnings("UnusedParameter")
public class RefBookDataServiceImpl implements RefBookDataService {

    private VersionRestService versionService;

    @Autowired
    public RefBookDataServiceImpl(VersionRestService versionService) {

        this.versionService = versionService;
    }

    @Override
    public Structure getDataStructure(Integer versionId, DataCriteria criteria) {

        return versionService.getStructure(versionId);
    }

    @Override
    public List<RefBookRowValue> getDataContent(List<RefBookRowValue> searchContent, DataCriteria criteria) {

        return searchContent;
    }
}

package ru.i_novus.ms.rdm.rest.client.impl;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.compare.ComparableRow;
import ru.i_novus.ms.rdm.api.model.compare.CompareDataCriteria;
import ru.i_novus.ms.rdm.api.model.diff.PassportDiff;
import ru.i_novus.ms.rdm.api.model.diff.RefBookDataDiff;
import ru.i_novus.ms.rdm.api.model.diff.StructureDiff;
import ru.i_novus.ms.rdm.api.service.CompareService;
import ru.i_novus.ms.rdm.rest.client.feign.CompareServiceFeignClient;

public class CompareServiceRestClient implements CompareService {

    private final CompareServiceFeignClient client;

    public CompareServiceRestClient(CompareServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public PassportDiff comparePassports(Integer oldVersionId, Integer newVersionId) {
        return client.comparePassports(oldVersionId, newVersionId);
    }

    @Override
    public StructureDiff compareStructures(Structure oldStructure, Structure newStructure) {
        return client.compareStructures(oldStructure, newStructure);
    }

    @Override
    public StructureDiff compareStructures(Integer oldVersionId, Integer newVersionId) {
        return client.compareStructures(oldVersionId, newVersionId);
    }

    @Override
    public RefBookDataDiff compareData(CompareDataCriteria compareDataCriteria) {
        return client.compareData(compareDataCriteria);
    }

    @Override
    public Page<ComparableRow> getCommonComparableRows(CompareDataCriteria criteria) {
        return client.getCommonComparableRows(criteria);
    }
}

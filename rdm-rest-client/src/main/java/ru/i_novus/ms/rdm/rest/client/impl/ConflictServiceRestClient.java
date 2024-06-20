package ru.i_novus.ms.rdm.rest.client.impl;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.enumeration.ConflictType;
import ru.i_novus.ms.rdm.api.model.conflict.DeleteRefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflict;
import ru.i_novus.ms.rdm.api.model.conflict.RefBookConflictCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.service.ConflictService;
import ru.i_novus.ms.rdm.rest.client.feign.ConflictServiceFeignClient;

import java.util.List;

public class ConflictServiceRestClient implements ConflictService {

    private final ConflictServiceFeignClient client;

    public ConflictServiceRestClient(ConflictServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public Page<RefBookConflict> search(RefBookConflictCriteria criteria) {
        return client.search(criteria);
    }

    @Override
    public Long countConflictedRowIds(RefBookConflictCriteria criteria) {
        return client.countConflictedRowIds(criteria);
    }

    @Override
    public Page<Long> searchConflictedRowIds(RefBookConflictCriteria criteria) {
        return client.searchConflictedRowIds(criteria);
    }

    @Override
    public void delete(Integer id) {
        client.delete(id);
    }

    @Override
    public void delete(DeleteRefBookConflictCriteria criteria) {
        client.delete(criteria);
    }

    @Override
    public List<Long> getReferrerConflictedIds(Integer referrerVersionId, List<Long> refRecordIds) {
        return client.getReferrerConflictedIds(referrerVersionId, refRecordIds);
    }

    @Override
    public List<RefBookVersion> getConflictingReferrers(Integer versionId, ConflictType conflictType) {
        return client.getConflictingReferrers(versionId, conflictType);
    }

    @Override
    public Boolean checkConflicts(Integer refFromId, Integer oldRefToId, Integer newRefToId, ConflictType conflictType) {
        return client.checkConflicts(refFromId, oldRefToId, newRefToId, conflictType);
    }

    @Override
    public void discoverConflicts(Integer oldVersionId, Integer newVersionId) {
        client.discoverConflicts(oldVersionId, newVersionId);
    }

    @Override
    public void copyConflicts(Integer oldVersionId, Integer newVersionId) {
        client.copyConflicts(oldVersionId, newVersionId);
    }
}

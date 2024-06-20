package ru.i_novus.ms.rdm.rest.client.impl;

import org.springframework.data.domain.Page;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExistsData;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.rest.client.feign.VersionRestServiceFeignClient;

import java.time.LocalDateTime;
import java.util.List;

public class VersionRestServiceRestClient implements VersionRestService {

    private final VersionRestServiceFeignClient client;

    public VersionRestServiceRestClient(VersionRestServiceFeignClient client) {
        this.client = client;
    }

    @Override
    public Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria) {
        return client.search(versionId, criteria);
    }

    @Override
    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {
        return client.getVersions(criteria);
    }

    @Override
    public RefBookVersion getById(Integer versionId) {
        return client.getById(versionId);
    }

    @Override
    public RefBookVersion getVersion(String version, String refBookCode) {
        return client.getVersion(version, refBookCode);
    }

    @Override
    public RefBookVersion getLastPublishedVersion(String refBookCode) {
        return client.getLastPublishedVersion(refBookCode);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria) {
        return client.search(refBookCode, date, criteria);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria) {
        return client.search(refBookCode, criteria);
    }

    @Override
    public Structure getStructure(Integer versionId) {
        return client.getStructure(versionId);
    }

    @Override
    public ExistsData existsData(List<String> rowIds) {
        return client.existsData(rowIds);
    }

    @Override
    public RefBookRowValue getRow(String rowId) {
        return client.getRow(rowId);
    }

    @Override
    public ExportFile getVersionFile(Integer versionId, FileType fileType) {
        return client.getVersionFile(versionId, fileType);
    }
}

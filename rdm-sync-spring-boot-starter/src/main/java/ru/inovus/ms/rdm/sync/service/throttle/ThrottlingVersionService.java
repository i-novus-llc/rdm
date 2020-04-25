package ru.inovus.ms.rdm.sync.service.throttle;

import org.springframework.data.domain.Page;
import ru.inovus.ms.rdm.api.enumeration.FileType;
import ru.inovus.ms.rdm.api.model.ExportFile;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.api.model.version.RefBookVersion;
import ru.inovus.ms.rdm.api.model.version.VersionCriteria;
import ru.inovus.ms.rdm.api.service.ExistsData;
import ru.inovus.ms.rdm.api.service.VersionService;

import java.time.LocalDateTime;
import java.util.List;

public class ThrottlingVersionService implements VersionService {

    private final Throttle throttle;
    private final VersionService versionService;

    public ThrottlingVersionService(Throttle throttle, VersionService versionService) {
        this.throttle = throttle;
        this.versionService = versionService;
    }

    public Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.search(versionId, criteria);
    }

    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.getVersions(criteria);
    }

    public RefBookVersion getById(Integer versionId) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.getById(versionId);
    }

    public RefBookVersion getVersion(String version, String refBookCode) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.getVersion(version, refBookCode);
    }

    public RefBookVersion getLastPublishedVersion(String refBookCode) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.getLastPublishedVersion(refBookCode);
    }

    public Page<RefBookRowValue> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.search(refBookCode, date, criteria);
    }

    public Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.search(refBookCode, criteria);
    }

    public Structure getStructure(Integer versionId) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.getStructure(versionId);
    }

    public ExportFile getVersionFile(Integer versionId, FileType fileType) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.getVersionFile(versionId, fileType);
    }

    public ExistsData existsData(List<String> rowIds) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.existsData(rowIds);
    }

    public RefBookRowValue getRow(String rowId) {
        throttle.throttleAndUpdateRequestTime();
        return versionService.getRow(rowId);
    }

}

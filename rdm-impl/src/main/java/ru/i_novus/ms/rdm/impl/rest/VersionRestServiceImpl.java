package ru.i_novus.ms.rdm.impl.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.enumeration.FileType;
import ru.i_novus.ms.rdm.api.model.ExportFile;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.RefBookRowValue;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.api.model.version.RefBookVersion;
import ru.i_novus.ms.rdm.api.model.version.VersionCriteria;
import ru.i_novus.ms.rdm.api.rest.VersionRestService;
import ru.i_novus.ms.rdm.api.service.ExistsData;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.util.TimeUtils;

import java.time.LocalDateTime;
import java.util.List;

@Primary
@Service
public class VersionRestServiceImpl implements VersionRestService {

    private VersionService versionService;

    @Autowired
    @SuppressWarnings("squid:S00107")
    public VersionRestServiceImpl(VersionService versionService) {

        this.versionService = versionService;
    }

    @Override
    public Page<RefBookRowValue> search(Integer versionId, SearchDataCriteria criteria) {
        return versionService.search(versionId, criteria);
    }

    /**
     * Получение списка версий справочника по параметрам критерия.
     *
     * @param criteria критерий поиска
     * @return Список версий справочника
     */
    @Override
    public Page<RefBookVersion> getVersions(VersionCriteria criteria) {
        return versionService.getVersions(criteria);
    }

    @Override
    public RefBookVersion getById(Integer versionId) {
        return versionService.getById(versionId);
    }

    @Override
    public RefBookVersion getVersion(String version, String refBookCode) {
        return versionService.getVersion(version, refBookCode);
    }

    @Override
    public RefBookVersion getLastPublishedVersion(String refBookCode) {
        return versionService.getLastPublishedVersion(refBookCode);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria) {
        return versionService.search(refBookCode, date, criteria);
    }

    @Override
    public Page<RefBookRowValue> search(String refBookCode, SearchDataCriteria criteria) {
        return search(refBookCode, TimeUtils.now(), criteria);
    }

    @Override
    public Structure getStructure(Integer versionId) {
        return versionService.getStructure(versionId);
    }

    @Override
    public ExistsData existsData(List<String> rowIds) {
        return versionService.existsData(rowIds);
    }

    @Override
    public RefBookRowValue getRow(String rowId) {
        return versionService.getRow(rowId);
    }

    @Override
    public ExportFile getVersionFile(Integer versionId, FileType fileType) {
        return versionService.getVersionFile(versionId, fileType);
    }
}

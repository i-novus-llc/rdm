package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.service.api.VersionPlainDataService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@Primary
public class VersionPlainDataServiceImpl implements VersionPlainDataService{


    private VersionService versionService;

    @Autowired
    public VersionPlainDataServiceImpl(VersionService versionService) {
        this.versionService = versionService;
    }

    @Override
    public Page<Map<String, String>> search(Integer versionId, SearchDataCriteria criteria) {
        return versionService.search(versionId, criteria).map(ConverterUtil::toStringMap);
    }

    @Override
    public Page<Map<String, String>> search(String refBookCode, OffsetDateTime date, SearchDataCriteria criteria) {
        return versionService.search(refBookCode, date, criteria).map(ConverterUtil::toStringMap);
    }

    @Override
    public Page<Map<String, String>> search(String refBookCode, SearchDataCriteria criteria) {
        return versionService.search(refBookCode,  criteria).map(ConverterUtil::toStringMap);
    }

    @Override
    public Map<String, String> gerRow(String rowId) {
        return ConverterUtil.toStringMap(versionService.gerRow(rowId));
    }
}

package ru.i_novus.ms.rdm.impl.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.i_novus.ms.rdm.api.service.VersionPlainDataService;
import ru.i_novus.ms.rdm.api.service.VersionService;
import ru.i_novus.ms.rdm.api.model.refdata.SearchDataCriteria;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@Transactional(readOnly = true)
@Primary
public class VersionPlainDataServiceImpl implements VersionPlainDataService {


    private VersionService versionService;

    @Autowired
    public VersionPlainDataServiceImpl(VersionService versionService) {
        this.versionService = versionService;
    }

    @Override
    public Page<Map<String, Object>> search(Integer versionId, SearchDataCriteria criteria) {
        return versionService.search(versionId, criteria).map(ConverterUtil::toStringObjectMap);
    }

    @Override
    public Page<Map<String, Object>> search(String refBookCode, LocalDateTime date, SearchDataCriteria criteria) {
        return versionService.search(refBookCode, date, criteria).map(ConverterUtil::toStringObjectMap);
    }

    @Override
    public Page<Map<String, Object>> search(String refBookCode, SearchDataCriteria criteria) {
        return versionService.search(refBookCode,  criteria).map(ConverterUtil::toStringObjectMap);
    }

    @Override
    public Map<String, Object> getRow(String rowId) {
        return ConverterUtil.toStringObjectMap(versionService.getRow(rowId));
    }
}

package ru.inovus.ms.rdm.service;

import io.swagger.annotations.ApiParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.service.api.VersionPlainDataService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.ConverterUtil;

import javax.ws.rs.BeanParam;
import java.time.OffsetDateTime;
import java.util.Map;

@Service
@Transactional(readOnly = true)
public class VersionPlainDataServiceImpl implements VersionPlainDataService{


    private VersionService versionService;

    @Autowired
    public VersionPlainDataServiceImpl(VersionService versionService) {
        this.versionService = versionService;
    }

    @Override
    public Page<Map<String, String>> search(Integer versionId, @BeanParam SearchDataCriteria criteria) {
        return versionService.search(versionId, criteria).map(ConverterUtil::toStringMap);
    }

    @Override
    public Page<Map<String, String>> search(@ApiParam("Код справочника") String refBookCode, @ApiParam("Дата получения данных") OffsetDateTime date, @BeanParam SearchDataCriteria criteria) {
        return versionService.search(refBookCode, date, criteria).map(ConverterUtil::toStringMap);
    }

    @Override
    public Page<Map<String, String>> search(@ApiParam("Код справочника") String refBookCode, @BeanParam SearchDataCriteria criteria) {
        return versionService.search(refBookCode,  criteria).map(ConverterUtil::toStringMap);
    }

    @Override
    public Map<String, String> gerRow(@ApiParam("Идентификатор строки") String rowId) {
        return ConverterUtil.toStringMap(versionService.gerRow(rowId));
    }
}

package ru.inovus.ms.rdm.service;

import net.n2oapp.criteria.api.CollectionPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.entity.RefBookVersionEntity;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.repositiory.RefBookVersionRepository;
import ru.inovus.ms.rdm.util.ConverterUtil;
import ru.inovus.ms.rdm.util.RowValuePage;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static ru.inovus.ms.rdm.util.ConverterUtil.localDateTimeToDate;

/**
 * Created by tnurdinov on 24.05.2018.
 */
@Service
public class VersionServiceImpl implements VersionService {

    private RefBookVersionRepository versionRepository;
    private SearchDataService searchDataService;
    private FieldFactory fieldFactory;

    @Autowired
    public VersionServiceImpl(RefBookVersionRepository versionRepository, SearchDataService searchDataService, FieldFactory fieldFactory) {
        this.versionRepository = versionRepository;
        this.searchDataService = searchDataService;
        this.fieldFactory = fieldFactory;
    }

    @Override
    public Page<RowValue> search(Integer versionId, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findOne(versionId);
        return getRowValuesOfVersion(criteria, version);
    }

    @Override
    public Page<RowValue> search(Integer refbookId, OffsetDateTime date, SearchDataCriteria criteria) {
        RefBookVersionEntity version = versionRepository.findActualOnDate(refbookId, date.toLocalDateTime());
        return version != null ? getRowValuesOfVersion(criteria, version) : new PageImpl<>(Collections.emptyList());
    }

    private Page<RowValue> getRowValuesOfVersion(SearchDataCriteria criteria, RefBookVersionEntity version) {
        List<Field> fields = ConverterUtil.structureToFields(version.getStructure(), fieldFactory);
        Date bdate = localDateTimeToDate(version.getFromDate());
        Date edate = localDateTimeToDate(version.getToDate());
        criteria = Optional.ofNullable(criteria).orElse(new SearchDataCriteria());
        DataCriteria dataCriteria = new DataCriteria(version.getStorageCode(), bdate, edate,
                fields, criteria.getFieldFilter(), criteria.getCommonFilter());
        CollectionPage<RowValue> pagedData = searchDataService.getPagedData(dataCriteria);
        return pagedData.getCollection() != null ? new RowValuePage(pagedData) : null;
    }

    @Override
    public Structure getMetadata(Integer versionId) {
        return versionRepository.findOne(versionId).getStructure();
    }
}

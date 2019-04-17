package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.util.TimeUtils.parseLocalDate;

@Controller
public class DataRecordController {

    @Autowired
    private VersionService versionService;
    @Autowired
    private DraftService draftService;

    public Map<String, Object> getRow(Integer versionId, Integer sysRecordId) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordId = new AttributeFilter("SYS_RECORDID", sysRecordId, FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordId)));
        Page<RefBookRowValue> search = versionService.search(versionId, criteria);

        if (isEmpty(search.getContent()))
            return emptyMap();

        LongRowValue rowValue = search.getContent().get(0);
        Map<String, Object> map = new HashMap<>();
        map.put("versionId", versionId);
        map.put("id", sysRecordId);
        rowValue.getFieldValues().forEach(fieldValue ->
                map.put(fieldValue.getField(), fieldValue.getValue()));
        return map;
    }

    public void updateData(Integer draftId, Row row) {
        row.getData().entrySet().stream()
                .filter(e -> e.getValue() instanceof Date)
                .forEach(e -> e.setValue(parseLocalDate(e.getValue())));
        draftService.updateData(draftId, row);
    }
}

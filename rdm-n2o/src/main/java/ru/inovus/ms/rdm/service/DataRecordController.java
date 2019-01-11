package ru.inovus.ms.rdm.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.FieldValue;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.i_novus.platform.datastorage.temporal.model.value.DateFieldValue;
import ru.inovus.ms.rdm.model.AttributeFilter;
import ru.inovus.ms.rdm.model.RefBookRowValue;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.SearchDataCriteria;
import ru.inovus.ms.rdm.service.api.DraftService;
import ru.inovus.ms.rdm.service.api.VersionService;
import ru.inovus.ms.rdm.util.TimeUtils;

import java.time.LocalDate;
import java.util.*;

import static java.time.format.DateTimeFormatter.ofPattern;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static ru.inovus.ms.rdm.util.TimeUtils.DATE_PATTERN_WITH_POINT;

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

        if (CollectionUtils.isEmpty(search.getContent()))
            return Collections.emptyMap();

        LongRowValue rowValue = search.getContent().get(0);
        Map<String, Object> map = new HashMap<>();
        map.put("versionId", versionId);
        map.put("id", sysRecordId);
        rowValue.getFieldValues().forEach(fieldValue ->
                map.put(fieldValue.getField(), toStringValue(fieldValue)));
        return map;
    }

    private Object toStringValue(FieldValue value) {
        Optional<Object> valueOptional = ofNullable(value).map(FieldValue::getValue);
        if (value instanceof DateFieldValue)
            return valueOptional.filter(o -> o instanceof LocalDate).map(o -> (LocalDate) o)
                    .map(localDate -> localDate.format(ofPattern(DATE_PATTERN_WITH_POINT)))
                    .orElse(null);
        return valueOptional.orElse(null);
    }

    public void updateData(Integer draftId, Row row) {
        row.getData().entrySet().stream()
                .filter(e -> e.getValue() instanceof Date)
                .forEach(e -> e.setValue(TimeUtils.parseLocalDate(e.getValue())));
        draftService.updateData(draftId, row);
    }
}

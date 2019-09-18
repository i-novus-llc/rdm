package ru.inovus.ms.rdm.n2o.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.model.DataConstants;
import ru.i_novus.platform.datastorage.temporal.model.LongRowValue;
import ru.inovus.ms.rdm.n2o.model.version.AttributeFilter;
import ru.inovus.ms.rdm.n2o.model.refdata.RefBookRowValue;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.refdata.SearchDataCriteria;
import ru.inovus.ms.rdm.n2o.service.api.DraftService;
import ru.inovus.ms.rdm.n2o.service.api.VersionService;

import java.util.*;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.addPrefix;
import static ru.inovus.ms.rdm.n2o.util.TimeUtils.parseLocalDate;

@Controller
@SuppressWarnings("unused")
public class DataRecordController {

    @Autowired
    private VersionService versionService;
    @Autowired
    private DraftService draftService;

    public Map<String, Object> getRow(Integer versionId, Integer sysRecordId) {

        SearchDataCriteria criteria = new SearchDataCriteria();
        AttributeFilter recordIdFilter = new AttributeFilter(DataConstants.SYS_PRIMARY_COLUMN, sysRecordId, FieldType.INTEGER);
        criteria.setAttributeFilter(singleton(singletonList(recordIdFilter)));

        Page<RefBookRowValue> search = versionService.search(versionId, criteria);
        if (isEmpty(search.getContent()))
            return emptyMap();

        LongRowValue rowValue = search.getContent().get(0);
        Map<String, Object> map = new HashMap<>();
        map.put("id", sysRecordId);
        map.put("versionId", versionId);

        rowValue.getFieldValues()
                .forEach(fieldValue ->
                        map.put(addPrefix(fieldValue.getField()), fieldValue.getValue()));
        return map;
    }

    @SuppressWarnings("WeakerAccess")
    public void updateData(Integer draftId, Row row) {
        row.getData().entrySet().stream()
                .filter(e -> e.getValue() instanceof Date)
                .forEach(e -> e.setValue(parseLocalDate(e.getValue())));

        draftService.updateData(draftId, row);
    }
}

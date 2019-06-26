package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.model.refdata.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by znurgaliev on 14.08.2018.
 */
public class DBPrimaryKeyValidation extends ErrorAttributeHolderValidation {

    public static final String DB_CONTAINS_PK_ERROR_CODE = "validation.db.contains.pk.err";

    private SearchDataService searchDataService;
    private Map<Structure.Attribute, Object> primaryKey;
    private String storageCode;
    private Long systemId;

    public DBPrimaryKeyValidation(SearchDataService searchDataService, Structure structure, Row row, String storageCode) {
        this.searchDataService = searchDataService;
        this.storageCode = storageCode;
        this.primaryKey = getPrimaryKeyMap(structure, row);
        this.systemId = row.getSystemId();
    }

    @Override
    public List<Message> validate() {
        if (!primaryKey.isEmpty()) {
            DataCriteria criteria = createCriteria();
            List<String> primaryKeyAttributes = primaryKey.keySet().stream()
                    .map(Structure.Attribute::getCode)
                    .collect(Collectors.toList());
            if (primaryKeyAttributes.stream().noneMatch(a -> getErrorAttributes().contains(a)) &&
                    searchDataService.getPagedData(criteria).getCollection()
                            .stream()
                            .anyMatch(rowValue -> !rowValue.getSystemId().equals(systemId))) {
                primaryKeyAttributes.forEach(this::addErrorAttribute);
                return Collections.singletonList(new Message(DB_CONTAINS_PK_ERROR_CODE,
                        primaryKey.entrySet().stream()
                                .map(entry -> entry.getKey().getName() + "\" - \"" + entry.getValue())
                                .collect(Collectors.joining("\", \""))));
            }
        }
        return Collections.emptyList();
    }

    private DataCriteria createCriteria() {
        List<Field> fields = primaryKey.keySet().stream().map(ConverterUtil::field)
                .collect(Collectors.toList());
        List<FieldSearchCriteria> filters = primaryKey.entrySet().stream()
                .map(entry -> new FieldSearchCriteria(ConverterUtil.field(entry.getKey()),
                        SearchTypeEnum.EXACT,
                        Collections.singletonList(ConverterUtil.toSearchType(entry.getValue()))))
                .collect(Collectors.toList());
        DataCriteria criteria = new DataCriteria(storageCode, null, null, fields, filters, null);
        criteria.setPage(1);
        criteria.setSize(systemId != null ? 2 : 1);
        return criteria;
    }

    private static Map<Structure.Attribute, Object> getPrimaryKeyMap(Structure structure, Row row) {
        Map<Structure.Attribute, Object> map = new HashMap<>();
        structure.getAttributes().stream()
                .filter(Structure.Attribute::getIsPrimary)
                .forEach(attribute -> map.put(attribute, row.getData().get(attribute.getCode())));
        return map;
    }
}

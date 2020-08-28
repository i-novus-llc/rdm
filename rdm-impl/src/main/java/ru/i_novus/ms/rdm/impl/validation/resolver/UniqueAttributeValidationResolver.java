package ru.i_novus.ms.rdm.impl.validation.resolver;

import net.n2oapp.platform.i18n.Message;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.version.UniqueAttributeValue;
import ru.i_novus.ms.rdm.impl.util.ConverterUtil;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.*;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

/**
 * Проверка на уникальность (включая проверку базы данных)
 */
public class UniqueAttributeValidationResolver implements AttributeValidationResolver<UniqueAttributeValue> {

    public static final String DB_CONTAINS_VALUE_EXCEPTION_CODE = "validation.db.contains.err";
    public static final String VALUE_NOT_UNIQUE_EXCEPTION_CODE = "validation.not.unique.err";
    private final Structure.Attribute attribute;
    private final SearchDataService searchDataService;
    private final String storageCode;

    private Set<Object> uniqueValues;


    public UniqueAttributeValidationResolver(Structure.Attribute attribute, SearchDataService searchDataService, String storageCode) {
        this.attribute = attribute;
        this.searchDataService = searchDataService;
        this.storageCode = storageCode;
        uniqueValues = new HashSet<>();
    }

    @Override
    public Message resolve(UniqueAttributeValue value) {
        if (value.getValue() != null) {
            if (!uniqueValues.add(value.getValue())) {
                return new Message(VALUE_NOT_UNIQUE_EXCEPTION_CODE, attribute.getName(), value.getValue());
            }

            if (!ofNullable(searchDataService.getPagedData(createCriteria(attribute, value)))
                    .map(rowValueCollectionPage ->
                            rowValueCollectionPage.getCollection()
                                    .stream()
                                    .filter(rowValue -> !rowValue.getSystemId().equals(value.getSystemId()))
                                    .collect(Collectors.toList()))
                    .map(Collection::isEmpty).orElse(true)) {
                return new Message(DB_CONTAINS_VALUE_EXCEPTION_CODE, attribute.getName(), value.getValue());
            }
        }
        return null;
    }

    private StorageDataCriteria createCriteria(Structure.Attribute attribute, UniqueAttributeValue value) {

        Field field = ConverterUtil.field(attribute);
        FieldSearchCriteria fieldSearchCriteria = new FieldSearchCriteria(field, SearchTypeEnum.EXACT,
                singletonList(ConverterUtil.toSearchValue(value.getValue())));

        StorageDataCriteria criteria = new StorageDataCriteria(storageCode, null, null,
                singletonList(field), singletonList(fieldSearchCriteria), null);
        criteria.setPage(BaseDataCriteria.MIN_PAGE);
        criteria.setSize(value.getSystemId() != null ? 2 : 1);
        return criteria;
    }
}

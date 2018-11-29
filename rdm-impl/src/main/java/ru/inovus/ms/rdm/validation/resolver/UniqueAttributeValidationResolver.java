package ru.inovus.ms.rdm.validation.resolver;

import net.n2oapp.criteria.api.CollectionPage;
import net.n2oapp.platform.i18n.Message;
import ru.i_novus.platform.datastorage.temporal.model.Field;
import ru.i_novus.platform.datastorage.temporal.model.criteria.DataCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.FieldSearchCriteria;
import ru.i_novus.platform.datastorage.temporal.model.criteria.SearchTypeEnum;
import ru.i_novus.platform.datastorage.temporal.service.SearchDataService;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

/**
 * Проверка на уникальность (включая проверку базы данных)
 */
public class UniqueAttributeValidationResolver implements AttributeValidationResolver {

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
    public Message resolve(Object value) {
        if (value != null) {
            if (!uniqueValues.add(value))
                return new Message(VALUE_NOT_UNIQUE_EXCEPTION_CODE, attribute.getName(), value);
            else if (!ofNullable(searchDataService.getPagedData(createCriteria(attribute, value)))
                    .map(CollectionPage::getCollection)
                    .map(Collection::isEmpty).orElse(true))
                return new Message(DB_CONTAINS_VALUE_EXCEPTION_CODE, attribute.getName(), value);
        }
        return null;
    }

    private DataCriteria createCriteria(Structure.Attribute attribute, Object value) {
        Field field = ConverterUtil.field(attribute);
        FieldSearchCriteria filter = new FieldSearchCriteria(field, SearchTypeEnum.EXACT,
                singletonList(ConverterUtil.toSearchType(value)));
        DataCriteria criteria = new DataCriteria(storageCode, null, null, singletonList(field), singletonList(filter), null);
        criteria.setPage(1);
        criteria.setSize(1);
        return criteria;
    }
}

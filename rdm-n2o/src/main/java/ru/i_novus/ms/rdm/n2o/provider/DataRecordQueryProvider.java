package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.query.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.query.field.QuerySimpleField;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.criteria.DataRecordCriteria;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordQueryResolver;
import ru.i_novus.ms.rdm.n2o.service.DataRecordController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addFieldProperty;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

/**
 * Провайдер для формирования запроса на получение данных
 * по конкретной записи из указанной версии справочника.
 */
@Service
public class DataRecordQueryProvider extends DataRecordBaseProvider implements DynamicMetadataProvider {

    static final String QUERY_PROVIDER_ID = "dataRecordQuery";

    private static final String CONTROLLER_CLASS_NAME = DataRecordController.class.getName();
    private static final String CONTROLLER_METHOD = "getRow";
    private static final String CRITERIA_CLASS_NAME = DataRecordCriteria.class.getName();

    private static final String CRITERIA_NAME = "criteria";
    private static final String MAPPING_CRITERIA_PREFIX = "['" + CRITERIA_NAME + "'].";

    @Autowired
    private Collection<DataRecordQueryResolver> resolvers;

    @Override
    public String getCode() {
        return QUERY_PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        DataRecordRequest request = toRequest(context);
        return singletonList(createQuery(request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oQuery.class);
    }

    /** Создание выборки. */
    private N2oQuery createQuery(DataRecordRequest request) {

        N2oQuery n2oQuery = new N2oQuery();
        n2oQuery.setUniques(new N2oQuery.Selection[]{ createSelection() });
        n2oQuery.setFields(createQueryFields(request));
        n2oQuery.setFilters(createQueryFilters(request));

        return n2oQuery;
    }

    /** Создание правила выборки. */
    private N2oQuery.Selection createSelection() {

        N2oJavaDataProvider provider = new N2oJavaDataProvider();
        provider.setClassName(CONTROLLER_CLASS_NAME);
        provider.setMethod(CONTROLLER_METHOD);
        provider.setSpringProvider(new SpringProvider());

        Argument criteriaArgument = new Argument();
        criteriaArgument.setName(CRITERIA_NAME);
        criteriaArgument.setType(Argument.Type.CRITERIA);
        criteriaArgument.setClassName(CRITERIA_CLASS_NAME);
        provider.setArguments(new Argument[]{ criteriaArgument });

        N2oQuery.Selection selection = new N2oQuery.Selection(N2oQuery.Selection.Type.list);
        selection.setResultMapping("#this");
        selection.setInvocation(provider);
        return selection;
    }

    /** Создание полей для выборки. */
    private QuerySimpleField[] createQueryFields(DataRecordRequest request) {

        return Stream.concat(
                createRegularFields(request).stream(),
                createDynamicFields(request.getStructure()).stream())
                .toArray(QuerySimpleField[]::new);
    }

    private List<QuerySimpleField> createRegularFields(DataRecordRequest request) {

        QuerySimpleField idField = new QuerySimpleField(FIELD_SYSTEM_ID);
        idField.setMapping("['" + FIELD_SYSTEM_ID + "']");

        QuerySimpleField versionIdField = new QuerySimpleField(FIELD_VERSION_ID);
        QuerySimpleField optLockValueField = new QuerySimpleField(FIELD_OPT_LOCK_VALUE);
        QuerySimpleField localeCodeField = new QuerySimpleField(FIELD_LOCALE_CODE);
        QuerySimpleField dataActionField = new QuerySimpleField(FIELD_DATA_ACTION);

        List<QuerySimpleField> list = new ArrayList<>(List.of(
                idField, versionIdField, optLockValueField, localeCodeField, dataActionField
        ));

        getSatisfiedResolvers(request.getDataAction())
                .map(resolver -> resolver.createRegularFields(request))
                .filter(fields -> !isEmpty(fields))
                .forEach(list::addAll);

        return list;
    }

    private List<QuerySimpleField> createDynamicFields(Structure structure) {

        if (structure.isEmpty())
            return emptyList();

        List<QuerySimpleField> list = new ArrayList<>(getDynamicFieldCount(structure));
        for (Structure.Attribute attribute : structure.getAttributes()) {

            switch (attribute.getType()) {

                case STRING, INTEGER, FLOAT, DATE, BOOLEAN ->
                    list.add(createField(attribute));

                case REFERENCE ->
                    list.addAll(createReferenceFields(attribute));

                default ->
                    throw new IllegalArgumentException("attribute type is not supported");
            }
        }
        return list;
    }

    /** Определение количества динамических полей по структуре. */
    private int getDynamicFieldCount(Structure structure) {

        return structure.getAttributes().size() + structure.getReferences().size();
    }

    private QuerySimpleField createField(Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());

        QuerySimpleField field = new QuerySimpleField(codeWithPrefix);
        field.setName(attribute.getName());
        field.setMapping(getAttributeMapping(codeWithPrefix));
        field.setDomain(N2oDomain.fieldTypeToDomain(attribute.getType()));

        return field;
    }

    private List<QuerySimpleField> createReferenceFields(Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());
        final String attributeMapping = getAttributeMapping(codeWithPrefix);

        QuerySimpleField valueField = new QuerySimpleField();
        valueField.setId(addFieldProperty(codeWithPrefix, REFERENCE_VALUE));
        valueField.setMapping(addFieldProperty(attributeMapping, REFERENCE_VALUE));
        valueField.setDomain(N2oDomain.STRING);

        QuerySimpleField displayValueField = new QuerySimpleField();
        displayValueField.setId(addFieldProperty(codeWithPrefix, REFERENCE_DISPLAY_VALUE));
        displayValueField.setMapping(addFieldProperty(attributeMapping, REFERENCE_DISPLAY_VALUE));
        displayValueField.setDomain(N2oDomain.STRING);

        return List.of(valueField, displayValueField);
    }

    private String getAttributeMapping(String attributeCode) {
        return "#this.get('" + attributeCode + "')";
    }

    /** Создание фильтров для выборки. */
    private N2oQuery.Filter[] createQueryFilters(DataRecordRequest request) {

        return createRegularFilters(request).toArray(N2oQuery.Filter[]::new);
    }

    private List<N2oQuery.Filter> createRegularFilters(DataRecordRequest request) {

        N2oQuery.Filter idFilter = new N2oQuery.Filter(FIELD_SYSTEM_ID, FilterType.eq);
        idFilter.setFieldId(FIELD_SYSTEM_ID);
        idFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_SYSTEM_ID);
        idFilter.setDomain(N2oDomain.INTEGER);

        N2oQuery.Filter versionIdFilter = new N2oQuery.Filter(FIELD_VERSION_ID, FilterType.eq);
        versionIdFilter.setFieldId(FIELD_VERSION_ID);
        versionIdFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_VERSION_ID);
        versionIdFilter.setDomain(N2oDomain.INTEGER);
        versionIdFilter.setDefaultValue(String.valueOf(request.getVersionId()));

        N2oQuery.Filter optLockValueFilter = new N2oQuery.Filter(FIELD_OPT_LOCK_VALUE, FilterType.eq);
        optLockValueFilter.setType(FilterType.eq);
        optLockValueFilter.setFieldId(FIELD_OPT_LOCK_VALUE);
        optLockValueFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_OPT_LOCK_VALUE);
        optLockValueFilter.setDomain(N2oDomain.INTEGER);
        optLockValueFilter.setDefaultValue(String.valueOf(DEFAULT_OPT_LOCK_VALUE));

        N2oQuery.Filter localeCodeFilter = new N2oQuery.Filter(FIELD_LOCALE_CODE, FilterType.eq);
        localeCodeFilter.setFieldId(FIELD_LOCALE_CODE);
        localeCodeFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_LOCALE_CODE);
        localeCodeFilter.setDomain(N2oDomain.STRING);
        localeCodeFilter.setDefaultValue(DEFAULT_LOCALE_CODE);

        N2oQuery.Filter dataActionFilter = new N2oQuery.Filter(FIELD_DATA_ACTION, FilterType.eq);
        dataActionFilter.setFieldId(FIELD_DATA_ACTION);
        dataActionFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_DATA_ACTION);
        dataActionFilter.setDomain(N2oDomain.STRING);

        List<N2oQuery.Filter> list = new ArrayList<>(List.of(
                idFilter, versionIdFilter, optLockValueFilter, localeCodeFilter, dataActionFilter
        ));

        getSatisfiedResolvers(request.getDataAction())
                .map(resolver -> resolver.createRegularFilters(request))
                .filter(fields -> !isEmpty(fields))
                .forEach(list::addAll);

        return list;
    }

    private Stream<DataRecordQueryResolver> getSatisfiedResolvers(String dataAction) {

        if (isEmpty(resolvers))
            return Stream.empty();

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(dataAction));
    }
}

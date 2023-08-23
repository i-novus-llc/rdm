package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
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

        final DataRecordRequest request = toRequest(context);
        return singletonList(createQuery(request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oQuery.class);
    }

    /** Создание выборки. */
    private N2oQuery createQuery(DataRecordRequest request) {

        final N2oQuery query = new N2oQuery();
        query.setUniques(new N2oQuery.Selection[]{ createSelection() });
        query.setFields(createQueryFields(request));

        return query;
    }

    /** Создание правила выборки. */
    private N2oQuery.Selection createSelection() {

        final N2oJavaDataProvider provider = new N2oJavaDataProvider();
        provider.setClassName(CONTROLLER_CLASS_NAME);
        provider.setMethod(CONTROLLER_METHOD);
        provider.setSpringProvider(new SpringProvider());

        final Argument criteriaArgument = new Argument();
        criteriaArgument.setName(CRITERIA_NAME);
        criteriaArgument.setType(Argument.Type.CRITERIA);
        criteriaArgument.setClassName(CRITERIA_CLASS_NAME);
        provider.setArguments(new Argument[]{ criteriaArgument });

        final N2oQuery.Selection selection = new N2oQuery.Selection(N2oQuery.Selection.Type.list);
        selection.setResultMapping("#this");
        selection.setInvocation(provider);
        return selection;
    }

    /** Создание полей для выборки. */
    private N2oQuery.Field[] createQueryFields(DataRecordRequest request) {

        return Stream.concat(
                        createRegularFields(request).stream(),
                        createDynamicFields(request.getStructure()).stream()
                )
                .toArray(N2oQuery.Field[]::new);
    }

    private List<N2oQuery.Field> createRegularFields(DataRecordRequest request) {

        final N2oQuery.Field idField = new N2oQuery.Field();
        idField.setId(FIELD_SYSTEM_ID);
        idField.setSelectMapping("['" + FIELD_SYSTEM_ID + "']");

        final N2oQuery.Filter idFilter = new N2oQuery.Filter();
        idFilter.setType(FilterType.eq);
        idFilter.setFilterField(FIELD_SYSTEM_ID);
        idFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_SYSTEM_ID);
        idFilter.setDomain(N2oDomain.INTEGER);
        idField.setFilterList(new N2oQuery.Filter[]{ idFilter });

        final N2oQuery.Field versionIdField = new N2oQuery.Field();
        versionIdField.setId(FIELD_VERSION_ID);

        final N2oQuery.Filter versionIdFilter = new N2oQuery.Filter();
        versionIdFilter.setType(FilterType.eq);
        versionIdFilter.setFilterField(FIELD_VERSION_ID);
        versionIdFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_VERSION_ID);
        versionIdFilter.setDomain(N2oDomain.INTEGER);
        versionIdFilter.setDefaultValue(String.valueOf(request.getVersionId()));
        versionIdField.setFilterList(new N2oQuery.Filter[]{ versionIdFilter });

        final N2oQuery.Field optLockValueField = new N2oQuery.Field();
        optLockValueField.setId(FIELD_OPT_LOCK_VALUE);

        final N2oQuery.Filter optLockValueFilter = new N2oQuery.Filter();
        optLockValueFilter.setType(FilterType.eq);
        optLockValueFilter.setFilterField(FIELD_OPT_LOCK_VALUE);
        optLockValueFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_OPT_LOCK_VALUE);
        optLockValueFilter.setDomain(N2oDomain.INTEGER);
        optLockValueFilter.setDefaultValue(String.valueOf(DEFAULT_OPT_LOCK_VALUE));
        optLockValueField.setFilterList(new N2oQuery.Filter[]{ optLockValueFilter });

        final N2oQuery.Field localeCodeField = new N2oQuery.Field();
        localeCodeField.setId(FIELD_LOCALE_CODE);

        final N2oQuery.Filter localeCodeFilter = new N2oQuery.Filter();
        localeCodeFilter.setType(FilterType.eq);
        localeCodeFilter.setFilterField(FIELD_LOCALE_CODE);
        localeCodeFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_LOCALE_CODE);
        localeCodeFilter.setDomain(N2oDomain.STRING);
        localeCodeFilter.setDefaultValue(DEFAULT_LOCALE_CODE);
        localeCodeField.setFilterList(new N2oQuery.Filter[]{ localeCodeFilter });

        final N2oQuery.Field dataActionField = new N2oQuery.Field();
        dataActionField.setId(FIELD_DATA_ACTION);

        final N2oQuery.Filter dataActionFilter = new N2oQuery.Filter();
        dataActionFilter.setType(FilterType.eq);
        dataActionFilter.setFilterField(FIELD_DATA_ACTION);
        dataActionFilter.setMapping(MAPPING_CRITERIA_PREFIX + FIELD_DATA_ACTION);
        dataActionFilter.setDomain(N2oDomain.STRING);
        dataActionField.setFilterList(new N2oQuery.Filter[]{ dataActionFilter });

        final List<N2oQuery.Field> list = new ArrayList<>(List.of(
                idField, versionIdField, optLockValueField, localeCodeField, dataActionField
        ));

        getSatisfiedResolvers(request.getDataAction())
                .map(resolver -> resolver.createRegularFields(request))
                .filter(fields -> !isEmpty(fields))
                .forEach(list::addAll);

        return list;
    }

    private List<N2oQuery.Field> createDynamicFields(Structure structure) {

        if (structure.isEmpty())
            return emptyList();

        List<N2oQuery.Field> list = new ArrayList<>(getDynamicFieldCount(structure));
        for (Structure.Attribute attribute : structure.getAttributes()) {

            switch (attribute.getType()) {

                case STRING: case INTEGER: case FLOAT: case DATE: case BOOLEAN:
                    list.add(createField(attribute));
                    break;

                case REFERENCE:
                    list.addAll(createReferenceFields(attribute));
                    break;

                default:
                    throw new IllegalArgumentException(String.format("Unknown attribute type in: %s", attribute));
            }
        }
        return list;
    }

    /** Определение количества динамических полей по структуре. */
    private int getDynamicFieldCount(Structure structure) {

        return structure.getAttributes().size() + structure.getReferences().size();
    }

    private  N2oQuery.Field createField(Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());

        N2oQuery.Field field = new N2oQuery.Field();
        field.setId(codeWithPrefix);
        field.setName(attribute.getName());
        field.setSelectMapping(getAttributeMapping(codeWithPrefix));
        field.setDomain(N2oDomain.fieldTypeToDomain(attribute.getType()));

        return field;
    }

    private List<N2oQuery.Field> createReferenceFields(Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());
        final String attributeMapping = getAttributeMapping(codeWithPrefix);

        final N2oQuery.Field valueField = new N2oQuery.Field();
        valueField.setId(addFieldProperty(codeWithPrefix, REFERENCE_VALUE));
        valueField.setSelectMapping(addFieldProperty(attributeMapping, REFERENCE_VALUE));
        valueField.setDomain(N2oDomain.STRING);

        final N2oQuery.Field displayValueField = new N2oQuery.Field();
        displayValueField.setId(addFieldProperty(codeWithPrefix, REFERENCE_DISPLAY_VALUE));
        displayValueField.setSelectMapping(addFieldProperty(attributeMapping, REFERENCE_DISPLAY_VALUE));
        displayValueField.setDomain(N2oDomain.STRING);

        return List.of(valueField, displayValueField);
    }

    private String getAttributeMapping(String attributeCode) {
        return "#this.get('" + attributeCode + "')";
    }

    private Stream<DataRecordQueryResolver> getSatisfiedResolvers(String dataAction) {

        if (isEmpty(resolvers))
            return Stream.empty();

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(dataAction));
    }
}

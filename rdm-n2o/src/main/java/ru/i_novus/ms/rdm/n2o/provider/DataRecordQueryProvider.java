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

    private N2oQuery createQuery(DataRecordRequest request) {

        N2oQuery n2oQuery = new N2oQuery();
        n2oQuery.setUniques(new N2oQuery.Selection[]{ createSelection() });
        n2oQuery.setFields(createQueryFields(request));

        return n2oQuery;
    }

    private N2oQuery.Selection createSelection() {

        N2oJavaDataProvider provider = new N2oJavaDataProvider();
        provider.setClassName(CONTROLLER_CLASS_NAME);
        provider.setMethod(CONTROLLER_METHOD);
        provider.setSpringProvider(new SpringProvider());

        Argument criteriaArgument = new Argument();
        criteriaArgument.setType(Argument.Type.CRITERIA);
        criteriaArgument.setClassName(CRITERIA_CLASS_NAME);
        provider.setArguments(new Argument[]{ criteriaArgument });

        N2oQuery.Selection selection = new N2oQuery.Selection(N2oQuery.Selection.Type.list);
        selection.setResultMapping("#this");
        selection.setInvocation(provider);
        return selection;
    }

    private N2oQuery.Field[] createQueryFields(DataRecordRequest request) {

        return Stream.concat(
                createRegularFields(request).stream(),
                createDynamicFields(request.getStructure()).stream())
                .toArray(N2oQuery.Field[]::new);
    }

    private List<N2oQuery.Field> createRegularFields(DataRecordRequest request) {

        N2oQuery.Field idField = new N2oQuery.Field();
        idField.setId(FIELD_SYSTEM_ID);
        idField.setSelectMapping("['" + FIELD_SYSTEM_ID + "']");

        N2oQuery.Filter idFilter = new N2oQuery.Filter();
        idFilter.setType(FilterType.eq);
        idFilter.setFilterField(FIELD_SYSTEM_ID);
        idFilter.setMapping(FIELD_SYSTEM_ID);
        idFilter.setDomain(N2oDomain.INTEGER);
        idField.setFilterList(new N2oQuery.Filter[]{ idFilter });

        N2oQuery.Field versionIdField = new N2oQuery.Field();
        versionIdField.setId(FIELD_VERSION_ID);

        N2oQuery.Filter versionIdFilter = new N2oQuery.Filter();
        versionIdFilter.setType(FilterType.eq);
        versionIdFilter.setFilterField(FIELD_VERSION_ID);
        versionIdFilter.setMapping(FIELD_VERSION_ID);
        versionIdFilter.setDomain(N2oDomain.INTEGER);
        versionIdFilter.setDefaultValue(String.valueOf(request.getVersionId()));
        versionIdField.setFilterList(new N2oQuery.Filter[]{ versionIdFilter });

        N2oQuery.Field optLockValueField = new N2oQuery.Field();
        optLockValueField.setId(FIELD_OPT_LOCK_VALUE);

        N2oQuery.Filter optLockValueFilter = new N2oQuery.Filter();
        optLockValueFilter.setType(FilterType.eq);
        optLockValueFilter.setFilterField(FIELD_OPT_LOCK_VALUE);
        optLockValueFilter.setMapping(FIELD_OPT_LOCK_VALUE);
        optLockValueFilter.setDomain(N2oDomain.INTEGER);
        optLockValueFilter.setDefaultValue(String.valueOf(DEFAULT_OPT_LOCK_VALUE));
        optLockValueField.setFilterList(new N2oQuery.Filter[]{ optLockValueFilter });

        N2oQuery.Field localeCodeField = new N2oQuery.Field();
        localeCodeField.setId(FIELD_LOCALE_CODE);

        N2oQuery.Filter localeCodeFilter = new N2oQuery.Filter();
        localeCodeFilter.setType(FilterType.eq);
        localeCodeFilter.setFilterField(FIELD_LOCALE_CODE);
        localeCodeFilter.setMapping(FIELD_LOCALE_CODE);
        localeCodeFilter.setDomain(N2oDomain.STRING);
        localeCodeFilter.setDefaultValue(DEFAULT_LOCALE_CODE);
        localeCodeField.setFilterList(new N2oQuery.Filter[]{ localeCodeFilter });

        N2oQuery.Field dataActionField = new N2oQuery.Field();
        dataActionField.setId(FIELD_DATA_ACTION);

        N2oQuery.Filter dataActionFilter = new N2oQuery.Filter();
        dataActionFilter.setType(FilterType.eq);
        dataActionFilter.setFilterField(FIELD_DATA_ACTION);
        dataActionFilter.setMapping(FIELD_DATA_ACTION);
        dataActionFilter.setDomain(N2oDomain.STRING);
        dataActionField.setFilterList(new N2oQuery.Filter[]{ dataActionFilter });

        List<N2oQuery.Field> list = new ArrayList<>(List.of(
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

        List<N2oQuery.Field> list = new ArrayList<>();
        for (Structure.Attribute attribute : structure.getAttributes()) {

            switch (attribute.getType()) {
                case STRING:
                case INTEGER:
                case FLOAT:
                case DATE:
                case BOOLEAN:
                    list.add(createField(attribute));
                    break;

                case REFERENCE:
                    list.addAll(createReferenceFields(attribute));
                    break;

                default:
                    throw new IllegalArgumentException("attribute type is not supported");
            }
        }
        return list;
    }

    private N2oQuery.Field createField(Structure.Attribute attribute) {

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

        N2oQuery.Field valueField = new N2oQuery.Field();
        valueField.setId(addFieldProperty(codeWithPrefix, REFERENCE_VALUE));
        valueField.setSelectMapping(addFieldProperty(attributeMapping, REFERENCE_VALUE));
        valueField.setDomain(N2oDomain.STRING);

        N2oQuery.Field displayValueField = new N2oQuery.Field();
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

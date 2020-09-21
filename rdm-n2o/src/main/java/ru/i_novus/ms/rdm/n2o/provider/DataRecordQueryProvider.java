package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.service.DataRecordController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil.addFieldProperty;
import static ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil.addPrefix;

/**
 * Провайдер для формирования запроса на получение данных
 * по конкретной записи из указанной версии справочника.
 */
@Service
public class DataRecordQueryProvider extends DataRecordBaseProvider implements DynamicMetadataProvider {

    static final String QUERY_PROVIDER_ID = "dataRecordQuery";

    private static final String CONTROLLER_CLASS_NAME = DataRecordController.class.getName();
    private static final String CONTROLLER_METHOD = "getRow";

    @Override
    public String getCode() {
        return QUERY_PROVIDER_ID;
    }

    /**
     * @param context параметры провайдера в формате versionId, где
     *                  versionId - идентификатор версии справочника
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        Integer versionId = Integer.parseInt(context);
        Structure structure = getStructureOrNull(versionId);

        return singletonList(createQuery(versionId, structure));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oQuery.class);
    }

    @SuppressWarnings("WeakerAccess")
    public N2oQuery createQuery(Integer versionId, Structure structure) {

        N2oQuery n2oQuery = new N2oQuery();
        n2oQuery.setUniques(new N2oQuery.Selection[]{ createSelection() });
        n2oQuery.setFields(createQueryFields(versionId, structure));

        return n2oQuery;
    }

    private N2oQuery.Selection createSelection() {

        N2oJavaDataProvider provider = new N2oJavaDataProvider();
        provider.setClassName(CONTROLLER_CLASS_NAME);
        provider.setMethod(CONTROLLER_METHOD);
        provider.setSpringProvider(new SpringProvider());

        Argument versionIdArgument = new Argument();
        versionIdArgument.setType(Argument.Type.PRIMITIVE);
        versionIdArgument.setName(DataRecordConstants.FIELD_VERSION_ID);

        Argument sysRecordIdArgument = new Argument();
        sysRecordIdArgument.setType(Argument.Type.PRIMITIVE);
        sysRecordIdArgument.setName(DataRecordConstants.FIELD_SYS_RECORD_ID);

        Argument optLockValueArgument = new Argument();
        optLockValueArgument.setType(Argument.Type.PRIMITIVE);
        optLockValueArgument.setName(DataRecordConstants.FIELD_OPT_LOCK_VALUE);

        Argument dataActionArgument = new Argument();
        dataActionArgument.setType(Argument.Type.PRIMITIVE);
        dataActionArgument.setName(DataRecordConstants.FIELD_DATA_ACTION);

        provider.setArguments(new Argument[]{ versionIdArgument, sysRecordIdArgument, optLockValueArgument, dataActionArgument });

        N2oQuery.Selection selection = new N2oQuery.Selection(N2oQuery.Selection.Type.list);
        selection.setFilters(DataRecordConstants.FIELD_FILTERS);
        selection.setResultMapping("#this");
        selection.setInvocation(provider);
        return selection;
    }

    private N2oQuery.Field[] createQueryFields(Integer versionId, Structure structure) {

        N2oQuery.Field versionIdField = new N2oQuery.Field();
        versionIdField.setId(DataRecordConstants.FIELD_VERSION_ID);
        N2oQuery.Filter versionIdFilter = new N2oQuery.Filter();
        versionIdFilter.setType(FilterType.eq);
        versionIdFilter.setFilterField(DataRecordConstants.FIELD_VERSION_ID);
        versionIdFilter.setMapping("[0]");
        versionIdFilter.setDomain(N2oDomain.INTEGER);
        versionIdFilter.setDefaultValue(String.valueOf(versionId));
        versionIdField.setFilterList(new N2oQuery.Filter[]{ versionIdFilter });

        N2oQuery.Field sysRecordIdField = new N2oQuery.Field();
        sysRecordIdField.setId(DataRecordConstants.FIELD_SYSTEM_ID);
        sysRecordIdField.setSelectMapping("['" + DataRecordConstants.FIELD_SYSTEM_ID + "']");
        N2oQuery.Filter sysRecordIdFilter = new N2oQuery.Filter();
        sysRecordIdFilter.setType(FilterType.eq);
        sysRecordIdFilter.setFilterField(DataRecordConstants.FIELD_SYS_RECORD_ID);
        sysRecordIdFilter.setMapping("[1]");
        sysRecordIdFilter.setDomain(N2oDomain.INTEGER);
        sysRecordIdField.setFilterList(new N2oQuery.Filter[]{ sysRecordIdFilter });

        N2oQuery.Field optLockValueField = new N2oQuery.Field();
        optLockValueField.setId(DataRecordConstants.FIELD_OPT_LOCK_VALUE);
        N2oQuery.Filter optLockValueFilter = new N2oQuery.Filter();
        optLockValueFilter.setType(FilterType.eq);
        optLockValueFilter.setFilterField(DataRecordConstants.FIELD_OPT_LOCK_VALUE);
        optLockValueFilter.setMapping("[2]");
        optLockValueFilter.setDomain(N2oDomain.INTEGER);
        optLockValueFilter.setDefaultValue(String.valueOf(DataRecordConstants.DEFAULT_OPT_LOCK_VALUE));
        optLockValueField.setFilterList(new N2oQuery.Filter[]{ optLockValueFilter });

        N2oQuery.Field dataActionField = new N2oQuery.Field();
        dataActionField.setId(DataRecordConstants.FIELD_DATA_ACTION);
        N2oQuery.Filter dataActionFilter = new N2oQuery.Filter();
        dataActionFilter.setType(FilterType.eq);
        dataActionFilter.setFilterField(DataRecordConstants.FIELD_DATA_ACTION);
        dataActionFilter.setMapping("[3]");
        dataActionFilter.setDomain(N2oDomain.STRING);
        dataActionField.setFilterList(new N2oQuery.Filter[]{ dataActionFilter });

        return Stream.concat(
                Stream.of(versionIdField, sysRecordIdField, optLockValueField, dataActionField),
                createDynamicFields(structure).stream())
                .toArray(N2oQuery.Field[]::new);
    }

    private List<N2oQuery.Field> createDynamicFields(Structure structure) {

        if (isEmptyStructure(structure)) {
            return emptyList();
        }

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
        valueField.setId(addFieldProperty(codeWithPrefix, DataRecordConstants.REFERENCE_VALUE));
        valueField.setSelectMapping(addFieldProperty(attributeMapping, DataRecordConstants.REFERENCE_VALUE));
        valueField.setDomain(N2oDomain.STRING);

        N2oQuery.Field displayValueField = new N2oQuery.Field();
        displayValueField.setId(addFieldProperty(codeWithPrefix, DataRecordConstants.REFERENCE_DISPLAY_VALUE));
        displayValueField.setSelectMapping(addFieldProperty(attributeMapping, DataRecordConstants.REFERENCE_DISPLAY_VALUE));
        displayValueField.setDomain(N2oDomain.STRING);

        return List.of(valueField, displayValueField);
    }

    private String getAttributeMapping(String attributeCode) {
        return "#this.get('" + attributeCode + "')";
    }
}

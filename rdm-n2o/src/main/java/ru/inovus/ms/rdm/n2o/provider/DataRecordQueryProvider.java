package ru.inovus.ms.rdm.n2o.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.n2o.service.DataRecordController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.addFieldProperty;
import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.addPrefix;

@Service
public class DataRecordQueryProvider implements DynamicMetadataProvider {

    static final String QUERY_PROVIDER_ID = "dataRecordQuery";

    private static final String CONTROLLER_CLASS_NAME = DataRecordController.class.getName();
    private static final String CONTROLLER_METHOD = "getRow";

    static final String VERSION_ID_NAME = "versionId";
    static final String SYS_RECORD_ID_NAME = "sysRecordId";

    public static final String REFERENCE_VALUE = "value";
    public static final String REFERENCE_DISPLAY_VALUE = "displayValue";

    @Autowired
    private VersionService versionService;

    @Override
    public String getCode() {
        return QUERY_PROVIDER_ID;
    }

    /**
     * @param context Контекст: ID версии
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        Integer versionId = Integer.parseInt(context);
        Structure structure = versionService.getStructure(versionId);

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

        Argument versionId = new Argument();
        versionId.setType(Argument.Type.PRIMITIVE);
        versionId.setName(VERSION_ID_NAME);

        Argument sysRecordId = new Argument();
        sysRecordId.setType(Argument.Type.PRIMITIVE);
        sysRecordId.setName(SYS_RECORD_ID_NAME);

        provider.setArguments(new Argument[]{ versionId, sysRecordId });

        N2oQuery.Selection selection = new N2oQuery.Selection(N2oQuery.Selection.Type.list);
        selection.setFilters(VERSION_ID_NAME + "," + SYS_RECORD_ID_NAME);
        selection.setResultMapping("#this");
        selection.setInvocation(provider);
        return selection;
    }

    private N2oQuery.Field[] createQueryFields(Integer versionId, Structure structure) {

        N2oQuery.Field versionIdField = new N2oQuery.Field();
        versionIdField.setId(VERSION_ID_NAME);
        N2oQuery.Filter versionIdFilter = new N2oQuery.Filter();
        versionIdFilter.setType(FilterType.eq);
        versionIdFilter.setFilterField(VERSION_ID_NAME);
        versionIdFilter.setMapping("[0]");
        versionIdFilter.setDomain(N2oDomain.INTEGER);
        versionIdFilter.setDefaultValue(String.valueOf(versionId));
        versionIdField.setFilterList(new N2oQuery.Filter[]{ versionIdFilter });

        N2oQuery.Field recordIdField = new N2oQuery.Field();
        recordIdField.setId("id");
        recordIdField.setSelectMapping("['id']");
        N2oQuery.Filter recordIdFilter = new N2oQuery.Filter();
        recordIdFilter.setType(FilterType.eq);
        recordIdFilter.setFilterField(SYS_RECORD_ID_NAME);
        recordIdFilter.setMapping("[1]");
        recordIdFilter.setDomain(N2oDomain.INTEGER);
        recordIdField.setFilterList(new N2oQuery.Filter[]{ recordIdFilter });

        return Stream.concat(
                Stream.of(versionIdField, recordIdField),
                createDynamicFields(structure).stream())
                .toArray(N2oQuery.Field[]::new);
    }

    private List<N2oQuery.Field> createDynamicFields(Structure structure) {

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
                    throw new IllegalArgumentException("attribute type not supported");
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
}

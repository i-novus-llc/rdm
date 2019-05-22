package ru.inovus.ms.rdm.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.invocation.java.SpringInvocation;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.DataRecordController;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Stream.of;
import static ru.inovus.ms.rdm.RdmUiUtil.addPrefix;

@Service
public class DataRecordQueryProvider implements DynamicMetadataProvider {

    static final String QUERY_PROVIDER_ID = "dataRecordQuery";

    private static final String CONTROLLER_CLASS_NAME = DataRecordController.class.getName();
    private static final String CONTROLLER_METHOD = "getRow";
    private static final String VERSION_ID_NAME = "versionId";

    @Autowired
    private VersionService versionService;

    @Override
    public String getCode() {
        return QUERY_PROVIDER_ID;
    }

    /**
     * @param s Параметры провайдера (ID версии)
     */
    @Override
    public List<? extends SourceMetadata> read(String s) {

        Integer versionId = Integer.parseInt(s);
        Structure structure = versionService.getStructure(versionId);

        return singletonList(createQuery(versionId, structure));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oQuery.class);
    }

    public N2oQuery createQuery(Integer versionId, Structure structure) {
        N2oQuery n2oQuery = new N2oQuery();
        n2oQuery.setUniques(new N2oQuery.Selection[]{createSelection()});
        n2oQuery.setFields(createQueryFields(versionId, structure));
        return n2oQuery;
    }

    private N2oQuery.Selection createSelection() {
        Argument versionId = new Argument();
        versionId.setType(Argument.Type.PRIMITIVE);
        versionId.setName(VERSION_ID_NAME);

        Argument sysRecordId = new Argument();
        sysRecordId.setType(Argument.Type.PRIMITIVE);
        sysRecordId.setName("sysRecordId");

        SpringInvocation invocation = new SpringInvocation();
        invocation.setClassName(CONTROLLER_CLASS_NAME);
        invocation.setMethodName(CONTROLLER_METHOD);
        invocation.setArguments(new Argument[]{versionId, sysRecordId});

        N2oQuery.Selection selection = new N2oQuery.Selection(N2oQuery.Selection.Type.list);
        selection.setFilters(VERSION_ID_NAME + ",id");
        selection.setResultMapping("#this");
        selection.setInvocation(invocation);
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
        versionIdField.setFilterList(new N2oQuery.Filter[]{versionIdFilter});

        N2oQuery.Field recordIdField = new N2oQuery.Field();
        recordIdField.setId("id");
        recordIdField.setSelectMapping("['id']");
        N2oQuery.Filter recordIdFilter = new N2oQuery.Filter();
        recordIdFilter.setType(FilterType.eq);
        recordIdFilter.setFilterField("sysRecordId");
        recordIdFilter.setMapping("[1]");
        recordIdFilter.setDomain(N2oDomain.INTEGER);
        recordIdField.setFilterList(new N2oQuery.Filter[]{recordIdFilter});

        return Stream.concat(
                of(versionIdField, recordIdField),
                dynamicFields(structure).stream())
                .toArray(N2oQuery.Field[]::new);
    }

    private List<N2oQuery.Field> dynamicFields(Structure structure) {
        List<N2oQuery.Field> list = new ArrayList<>();
        for (Structure.Attribute attribute : structure.getAttributes()) {
            String codeWithPrefix = addPrefix(attribute.getCode());

            N2oQuery.Field field = new N2oQuery.Field();
            field.setId(codeWithPrefix);
            field.setName(attribute.getName());
            field.setSelectMapping(createAttributeMapping(attribute.getCode()));

            switch (attribute.getType()) {
                case INTEGER:
                case STRING:
                case FLOAT:
                case DATE:
                case BOOLEAN:
                    field.setDomain(N2oDomain.fieldTypeToDomain(attribute.getType()));
                    break;

                case REFERENCE:
                    // NB: field used as valueField
                    field.setId(codeWithPrefix + ".value");
                    field.setSelectMapping(createAttributeMapping(attribute.getCode()) + ".value");
                    field.setDomain(N2oDomain.STRING);
                    list.add(field);

                    N2oQuery.Field displayField = new N2oQuery.Field();
                    displayField.setId(codeWithPrefix + ".displayValue");
                    displayField.setSelectMapping(createAttributeMapping(attribute.getCode()) + ".displayValue");
                    displayField.setDomain(N2oDomain.STRING);
                    list.add(displayField);
                    continue;

                default:
                    throw new IllegalArgumentException("attribute type not supported");
            }
            list.add(field);
        }
        return list;
    }

    private String createAttributeMapping(String attributeCode) {
        return "#this.get('" + attributeCode + "')";
    }
}

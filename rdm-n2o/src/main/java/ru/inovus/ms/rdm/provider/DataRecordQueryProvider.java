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
    private static final String INTEGER_DOMAIN = "integer";
    private static final String STRING_DOMAIN = "string";


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

        N2oQuery.Field versionIdfield = new N2oQuery.Field();
        versionIdfield.setId(VERSION_ID_NAME);
        N2oQuery.Filter filter = new N2oQuery.Filter();
        filter.setType(FilterType.eq);
        filter.setFilterField(VERSION_ID_NAME);
        filter.setMapping("[0]");
        filter.setDomain(INTEGER_DOMAIN);
        filter.setDefaultValue(String.valueOf(versionId));
        versionIdfield.setFilterList(new N2oQuery.Filter[]{filter});

        N2oQuery.Field idField = new N2oQuery.Field();
        idField.setId("id");
        idField.setSelectMapping("['id']");
        N2oQuery.Filter recordIdField = new N2oQuery.Filter();
        recordIdField.setType(FilterType.eq);
        recordIdField.setFilterField("sysRecordId");
        recordIdField.setMapping("[1]");
        recordIdField.setDomain(INTEGER_DOMAIN);
        idField.setFilterList(new N2oQuery.Filter[]{recordIdField});

        return Stream.concat(
                of(versionIdfield, idField),
                dynamicFields(structure).stream())
                .toArray(N2oQuery.Field[]::new);
    }

    private List<N2oQuery.Field> dynamicFields(Structure structure) {
        List<N2oQuery.Field> list = new ArrayList<>();
        for (Structure.Attribute attribute : structure.getAttributes()) {
            N2oQuery.Field field = new N2oQuery.Field();
            String codeWithPrefix = addPrefix(attribute.getCode());
            field.setId(codeWithPrefix);
            field.setName(attribute.getName());
            field.setSelectMapping(createAttributeMapping(attribute.getCode()));
            switch (attribute.getType()) {
                case INTEGER:
                    field.setDomain(INTEGER_DOMAIN);
                    break;
                case FLOAT:
                    field.setDomain("numeric");
                    break;
                case STRING:
                    field.setDomain(STRING_DOMAIN);
                    break;
                case BOOLEAN:
                    field.setDomain("boolean");
                    break;
                case DATE:
                    field.setDomain("date");
                    break;
                case REFERENCE:
                    field.setId(codeWithPrefix + ".value");
                    field.setSelectMapping(createAttributeMapping(attribute.getCode()) + ".value");
                    field.setDomain(STRING_DOMAIN);
                    list.add(field);
                    N2oQuery.Field field1 = new N2oQuery.Field();
                    field1.setId(codeWithPrefix + ".displayValue");
                    field1.setSelectMapping(createAttributeMapping(attribute.getCode()) + ".displayValue");
                    field1.setDomain(STRING_DOMAIN);
                    list.add(field1);
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

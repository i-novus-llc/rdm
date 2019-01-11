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

@Service
public class DataRecordQueryProvider implements DynamicMetadataProvider {

    final static String QUERY_PROVIDER_ID = "dataRecordQuery";

    private final static String CONTROLLER_CLASS_NAME = DataRecordController.class.getName();
    private final static String CONTROLLER_METHOD = "getRow";


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
        versionId.setName("versionId");

        Argument sysRecordId = new Argument();
        sysRecordId.setType(Argument.Type.PRIMITIVE);
        sysRecordId.setName("sysRecordId");

        SpringInvocation invocation = new SpringInvocation();
        invocation.setClassName(CONTROLLER_CLASS_NAME);
        invocation.setMethodName(CONTROLLER_METHOD);
        invocation.setArguments(new Argument[]{versionId, sysRecordId});

        N2oQuery.Selection selection = new N2oQuery.Selection(N2oQuery.Selection.Type.list);
        selection.setFilters("versionId,id");
        selection.setResultMapping("#this");
        selection.setInvocation(invocation);
        return selection;
    }


    private N2oQuery.Field[] createQueryFields(Integer versionId, Structure structure) {

        N2oQuery.Field versionIdfield = new N2oQuery.Field();
        versionIdfield.setId("versionId");
        N2oQuery.Filter filter = new N2oQuery.Filter();
        filter.setType(FilterType.eq);
        filter.setFilterField("versionId");
        filter.setMapping("[0]");
        filter.setDomain("integer");
        filter.setDefaultValue(String.valueOf(versionId));
        versionIdfield.setFilterList(new N2oQuery.Filter[]{filter});

        N2oQuery.Field idField = new N2oQuery.Field();
        idField.setId("id");
        idField.setSelectMapping("['id']");
        N2oQuery.Filter recordIdField = new N2oQuery.Filter();
        recordIdField.setType(FilterType.eq);
        recordIdField.setFilterField("sysRecordId");
        recordIdField.setMapping("[1]");
        recordIdField.setDomain("integer");
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
            field.setId(attribute.getCode());
            field.setName(attribute.getName());
            field.setSelectMapping("#this.get('" + attribute.getCode() + "')");
            switch (attribute.getType()) {
                case INTEGER:
                    field.setDomain("integer");
                    break;
                case FLOAT:
                    field.setDomain("numeric");
                    break;
                case STRING:
                    field.setDomain("string");
                    break;
                case BOOLEAN:
                    field.setDomain("boolean");
                    break;
                case DATE:
                    field.setDomain("date");
                    break;
                case REFERENCE:
                    field.setId(attribute.getCode() + ".value");
                    field.setSelectMapping("#this.get('" + attribute.getCode() + "').value");
                    field.setDomain("string");
                    list.add(field);
                    N2oQuery.Field field1 = new N2oQuery.Field();
                    field1.setId(attribute.getCode() + ".displayValue");
                    field1.setSelectMapping("#this.get('" + attribute.getCode() + "').displayValue");
                    field1.setDomain("string");
                    list.add(field1);
                    continue;
            }
            list.add(field);
        }
        return list;
    }


}

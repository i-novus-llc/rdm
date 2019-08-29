package ru.inovus.ms.rdm.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.AbstractDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.refdata.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.CreateDraftController;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Stream.of;
import static ru.inovus.ms.rdm.RdmUiUtil.addPrefix;

@Service
public class DataRecordObjectProvider implements DynamicMetadataProvider {

    static final String OBJECT_PROVIDER_ID = "dataRecordObject";

    @Autowired
    private VersionService versionService;


    @Override
    public String getCode() {
        return OBJECT_PROVIDER_ID;
    }

    /**
     * @param s Параметры провайдера (ID версии)
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String s) {

        Integer versionId = Integer.parseInt(s);
        Structure structure = versionService.getStructure(versionId);

        N2oObject n2oObject = new N2oObject();
        n2oObject.setOperations(new N2oObject.Operation[]{
                getCreateOperation(versionId, structure),
                getUpdateOperation(versionId, structure)});
        return singletonList(n2oObject);
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oObject.class);
    }

    private N2oObject.Operation getCreateOperation(Integer versionId, Structure structure) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("create");
        operation.setFormSubmitLabel("Сохранить");
        operation.setInvocation(createInvocation());
        operation.setInParameters(Stream.concat(
                of(versionIdParameter(versionId)), createDynamicParams(structure).stream())
                .toArray(N2oObject.Parameter[]::new));
        return operation;
    }

    private N2oObject.Operation getUpdateOperation(Integer versionId, Structure structure) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("update");
        operation.setFormSubmitLabel("Изменить");
        operation.setInvocation(createInvocation());
        operation.setInParameters(Stream.concat(
                of(versionIdParameter(versionId), systemIdParameter()), createDynamicParams(structure).stream())
                .toArray(N2oObject.Parameter[]::new));
        return operation;
    }

    private AbstractDataProvider createInvocation() {

        N2oJavaDataProvider invocation = new N2oJavaDataProvider();
        invocation.setClassName(CreateDraftController.class.getName());
        invocation.setMethod("updateDataRecord");
        invocation.setSpringProvider(new SpringProvider());

        Argument draftId = new Argument();
        draftId.setType(Argument.Type.PRIMITIVE);
        draftId.setName("draftId");
        draftId.setClassName(Integer.class.getName());

        Argument row = new Argument();
        row.setType(Argument.Type.CLASS);
        row.setName("row");
        row.setClassName(Row.class.getName());
        invocation.setArguments(new Argument[]{draftId, row});

        return invocation;
    }

    private N2oObject.Parameter versionIdParameter(Integer versionId) {
        N2oObject.Parameter versionIdParameter = new N2oObject.Parameter();
        versionIdParameter.setId("versionId");
        versionIdParameter.setMapping("[0]");
        versionIdParameter.setDomain(N2oDomain.INTEGER);
        versionIdParameter.setDefaultValue(String.valueOf(versionId));
        return versionIdParameter;
    }

    private N2oObject.Parameter systemIdParameter() {
        N2oObject.Parameter systemId = new N2oObject.Parameter();
        systemId.setId("id");
        systemId.setDomain(N2oDomain.INTEGER);
        systemId.setMapping("[1].systemId");
        return systemId;
    }

    private List<N2oObject.Parameter> createDynamicParams(Structure structure) {
        return structure.getAttributes().stream()
                .map(this::createParam).collect(Collectors.toList());
    }

    private N2oObject.Parameter createParam(Structure.Attribute attribute) {
        String codeWithPrefix = addPrefix(attribute.getCode());

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId(codeWithPrefix);
        parameter.setMapping("[1].data['" + attribute.getCode() + "']");

        switch (attribute.getType()) {
            case STRING:
            case INTEGER:
            case FLOAT:
            case DATE:
            case BOOLEAN:
                parameter.setDomain(N2oDomain.fieldTypeToDomain(attribute.getType()));
                break;

            case REFERENCE:
                parameter.setId(codeWithPrefix + ".value");
                parameter.setDomain(N2oDomain.STRING);
                break;

            default:
                throw new IllegalArgumentException("attribute type not supported");
        }
        return parameter;
    }


}

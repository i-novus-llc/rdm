package ru.inovus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.AbstractDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.service.VersionService;
import ru.inovus.ms.rdm.n2o.service.CreateDraftController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.of;
import static ru.inovus.ms.rdm.n2o.util.RdmUiUtil.addPrefix;

@Service
public class DataRecordObjectProvider implements DynamicMetadataProvider {

    static final String OBJECT_PROVIDER_ID = "dataRecordObject";

    private static final String CONTROLLER_CLASS_NAME = CreateDraftController.class.getName();
    private static final String CONTROLLER_CLASS_METHOD = "updateDataRecord";

    @Autowired
    private VersionService versionService;

    @Override
    public String getCode() {
        return OBJECT_PROVIDER_ID;
    }

    /**
     * @param context Контекст: ID версии
     */
    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        Integer versionId = Integer.parseInt(context);
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
                Stream.of(versionIdParameter(versionId)),
                createDynamicParams(structure).stream())
                .toArray(N2oObject.Parameter[]::new));
        return operation;
    }

    private N2oObject.Operation getUpdateOperation(Integer versionId, Structure structure) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("update");
        operation.setFormSubmitLabel("Изменить");
        operation.setInvocation(createInvocation());
        operation.setInParameters(Stream.concat(
                of(versionIdParameter(versionId), systemIdParameter()),
                createDynamicParams(structure).stream())
                .toArray(N2oObject.Parameter[]::new));
        return operation;
    }

    private AbstractDataProvider createInvocation() {

        N2oJavaDataProvider invocation = new N2oJavaDataProvider();
        invocation.setClassName(CONTROLLER_CLASS_NAME);
        invocation.setMethod(CONTROLLER_CLASS_METHOD);
        invocation.setSpringProvider(new SpringProvider());

        Argument versionIdArgument = new Argument();
        versionIdArgument.setType(Argument.Type.PRIMITIVE);
        versionIdArgument.setName("versionId");
        versionIdArgument.setClassName(Integer.class.getName());

        Argument rowArgument = new Argument();
        rowArgument.setType(Argument.Type.CLASS);
        rowArgument.setName("row");
        rowArgument.setClassName(Row.class.getName());

        invocation.setArguments(new Argument[]{versionIdArgument, rowArgument});

        return invocation;
    }

    private N2oObject.Parameter versionIdParameter(Integer versionId) {

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId("versionId");
        parameter.setMapping("[0]");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(String.valueOf(versionId));
        return parameter;
    }

    private N2oObject.Parameter systemIdParameter() {

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId("id");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setMapping("[1].systemId");
        return parameter;
    }

    private List<N2oObject.Parameter> createDynamicParams(Structure structure) {

        return structure.getAttributes().stream().map(this::createParam).collect(toList());
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
                if (attribute.getType() == FieldType.BOOLEAN)
                    parameter.setDefaultValue("false");
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

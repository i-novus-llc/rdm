package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.exception.SeverityType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oConstraint;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oValidation;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordObjectResolver;
import ru.i_novus.ms.rdm.n2o.service.DataRecordController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_SYSTEM_ID;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_VERSION_ID;
import static ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil.addPrefix;

/**
 * Провайдер для формирования объекта по выполнению операции
 * создания/изменения записи из указанной версии справочника.
 */
@Service
public class DataRecordObjectProvider extends DataRecordBaseProvider implements DynamicMetadataProvider {

    static final String OBJECT_PROVIDER_ID = "dataRecordObject";

    private static final String CONFLICT_VALIDATION_NAME = "checkDataConflicts";
    private static final String CONFLICT_VALIDATION_CLASS_NAME = DataRecordController.class.getName();
    private static final String CONFLICT_VALIDATION_CLASS_METHOD = "getDataConflicts";
    private static final String CONFLICT_TEXT_RESULT = "conflictText";

    @Autowired
    private Collection<DataRecordObjectResolver> resolvers;

    @Override
    public String getCode() {
        return OBJECT_PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        DataRecordRequest request = toRequest(context);
        return singletonList(createObject(request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oObject.class);
    }

    private N2oObject createObject(DataRecordRequest request) {

        N2oObject n2oObject = new N2oObject();
        n2oObject.setOperations(createOperations(request));

        return n2oObject;
    }

    private N2oObject.Operation[] createOperations(DataRecordRequest request) {

        List<N2oObject.Operation> operations = getSatisfiedResolvers(request.getDataAction())
                .map(resolver -> createOperation(resolver, request))
                .collect(toList());

        return operations.toArray(N2oObject.Operation[]::new);
    }

    private N2oObject.Operation createOperation(DataRecordObjectResolver resolver, DataRecordRequest request) {

        N2oObject.Operation operation = resolver.createOperation(request);
        operation.setInParameters(Stream.concat(
                resolver.createRegularParams(request).stream(),
                createDynamicParams(request, resolver.getRecordMappingIndex(request)).stream())
                .toArray(N2oObject.Parameter[]::new));

        return operation;
    }

    private List<N2oObject.Parameter> createDynamicParams(DataRecordRequest request, int mappingIndex) {

        Structure structure = request.getStructure();
        if (structure.isEmpty())
            return emptyList();

        return structure.getAttributes().stream()
                .map(attribute ->  createParam(attribute, mappingIndex))
                .collect(toList());
    }

    private N2oObject.Parameter createParam(Structure.Attribute attribute, int mappingIndex) {

        final String mappingArgumentFormat = "[%1$d].data['%2$s']";

        final String codeWithPrefix = addPrefix(attribute.getCode());

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setMapping(String.format(mappingArgumentFormat, mappingIndex, attribute.getCode()));

        switch (attribute.getType()) {
            case STRING:
            case INTEGER:
            case FLOAT:
            case DATE:
            case BOOLEAN:
                parameter.setId(codeWithPrefix);
                parameter.setDomain(N2oDomain.fieldTypeToDomain(attribute.getType()));
                enrichParam(parameter, attribute);
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

    /** Заполнение дополнительных полей параметра в зависимости от типа атрибута. */
    private void enrichParam(N2oObject.Parameter parameter, Structure.Attribute attribute) {

        switch (attribute.getType()) {
            case DATE:
                parameter.setNormalize("T(ru.i_novus.ms.rdm.api.util.TimeUtils).parseLocalDate(#this)");
                break;

            case BOOLEAN:
                parameter.setDefaultValue("false");
                break;

            default:
                break;
        }
    }

    /**
     * Добавление валидации о наличии конфликтов в записи справочника.
     * <p>
     * При наличии хотя бы одного конфликта выводит предупреждение
     * после открытия модальной формы на редактирование записи.
     *
     * @param versionId версия справочника
     * @param operation операция, выполняемая над записью
     */
    private void addDataConflictValidation(Integer versionId, N2oObject.Operation operation) {

        N2oJavaDataProvider dataProvider = new N2oJavaDataProvider();
        dataProvider.setClassName(CONFLICT_VALIDATION_CLASS_NAME);
        dataProvider.setMethod(CONFLICT_VALIDATION_CLASS_METHOD);
        dataProvider.setSpringProvider(new SpringProvider());

        Argument versionIdArgument = new Argument();
        versionIdArgument.setType(Argument.Type.PRIMITIVE);
        versionIdArgument.setClassName("java.lang.Integer");
        versionIdArgument.setName("versionId");

        Argument idArgument = new Argument();
        idArgument.setType(Argument.Type.PRIMITIVE);
        idArgument.setClassName("java.lang.Long");
        idArgument.setName("id");

        dataProvider.setArguments(new Argument[]{ versionIdArgument, idArgument });

        N2oConstraint constraint = new N2oConstraint();
        constraint.setId(CONFLICT_VALIDATION_NAME);
        constraint.setSeverity(SeverityType.warning);
        constraint.setServerMoment(N2oValidation.ServerMoment.beforeQuery);
        constraint.setResult("#this == null");
        constraint.setMessage('{' + CONFLICT_TEXT_RESULT + '}');
        constraint.setN2oInvocation(dataProvider);

        N2oObject.Parameter versionIdParam = new N2oObject.Parameter(
                N2oObject.Parameter.Type.in, FIELD_VERSION_ID, "[0]");
        versionIdParam.setDefaultValue(versionId.toString());
        versionIdParam.setDomain(N2oDomain.INTEGER);

        N2oObject.Parameter idParam = new N2oObject.Parameter(
                N2oObject.Parameter.Type.in, FIELD_SYSTEM_ID, "[1]");
        idParam.setDomain(N2oDomain.LONG);
        constraint.setInParameters(new N2oObject.Parameter[]{ versionIdParam, idParam });

        N2oObject.Parameter conflictTextParam = new N2oObject.Parameter(
                N2oObject.Parameter.Type.out, CONFLICT_TEXT_RESULT, "(#this)");
        conflictTextParam.setDomain(N2oDomain.STRING);
        N2oObject.Parameter[] outParams = new N2oObject.Parameter[]{ conflictTextParam };
        constraint.setOutParameters(outParams);

        N2oObject.Operation.Validations validations = new N2oObject.Operation.Validations();
        validations.setInlineValidations(new N2oValidation[]{ constraint });
        operation.setValidations(validations);
    }

    private Stream<DataRecordObjectResolver> getSatisfiedResolvers(String dataAction) {

        if (isEmpty(resolvers))
            return Stream.empty();

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(dataAction));
    }
}

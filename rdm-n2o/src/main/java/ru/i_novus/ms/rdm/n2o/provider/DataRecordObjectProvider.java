package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.exception.SeverityType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.dataprovider.AbstractDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oConstraint;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oValidation;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.n2o.service.CreateDraftController;
import ru.i_novus.ms.rdm.n2o.service.DataRecordController;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil.addPrefix;

/**
 * Провайдер для формирования объекта по выполнению операции
 * создания/изменения записи из указанной версии справочника.
 */
@Service
public class DataRecordObjectProvider extends DataRecordBaseProvider implements DynamicMetadataProvider {

    static final String OBJECT_PROVIDER_ID = "dataRecordObject";

    private static final String CONTROLLER_CLASS_NAME = CreateDraftController.class.getName();
    private static final String CONTROLLER_CLASS_METHOD = "updateDataRecord";

    private static final String CONFLICT_VALIDATION_NAME = "checkDataConflicts";
    private static final String CONFLICT_VALIDATION_CLASS_NAME = DataRecordController.class.getName();
    private static final String CONFLICT_VALIDATION_CLASS_METHOD = "getDataConflicts";
    private static final String CONFLICT_TEXT_RESULT = "conflictText";

    @Override
    public String getCode() {
        return OBJECT_PROVIDER_ID;
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

        return singletonList(createObject(versionId, structure));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oObject.class);
    }

    private N2oObject createObject(Integer versionId, Structure structure) {

        N2oObject n2oObject = new N2oObject();
        n2oObject.setOperations(new N2oObject.Operation[]{
                getCreateOperation(versionId, structure),
                getUpdateOperation(versionId, structure)
        });

        return n2oObject;
    }

    private N2oObject.Operation getCreateOperation(Integer versionId, Structure structure) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("create");
        operation.setFormSubmitLabel("Сохранить");

        operation.setInvocation(createInvocation());
        operation.setInParameters(Stream.concat(
                Stream.of(createVersionIdParameter(versionId),
                        createOptLockValueParameter()),
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
                Stream.of(createVersionIdParameter(versionId),
                        createSystemIdParameter(),
                        createOptLockValueParameter()),
                createDynamicParams(structure).stream())
                .toArray(N2oObject.Parameter[]::new));

        addDataConflictValidation(versionId, operation);

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

        Argument optLockValueArgument = new Argument();
        optLockValueArgument.setType(Argument.Type.PRIMITIVE);
        optLockValueArgument.setName(DataRecordConstants.FIELD_OPT_LOCK_VALUE);
        optLockValueArgument.setClassName(Integer.class.getName());

        invocation.setArguments(new Argument[]{ versionIdArgument, rowArgument, optLockValueArgument });

        return invocation;
    }

    private N2oObject.Parameter createVersionIdParameter(Integer versionId) {

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId(DataRecordConstants.FIELD_VERSION_ID);
        parameter.setMapping("[0]");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(String.valueOf(versionId));
        return parameter;
    }

    private N2oObject.Parameter createSystemIdParameter() {

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId(DataRecordConstants.FIELD_SYSTEM_ID);
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setMapping("[1].systemId");
        return parameter;
    }

    private N2oObject.Parameter createOptLockValueParameter() {
        N2oObject.Parameter optLockValueParameter = new N2oObject.Parameter();
        optLockValueParameter.setId(DataRecordConstants.FIELD_OPT_LOCK_VALUE);
        optLockValueParameter.setMapping("[2]");
        optLockValueParameter.setDomain(N2oDomain.INTEGER);
        optLockValueParameter.setDefaultValue(String.valueOf(DataRecordConstants.DEFAULT_OPT_LOCK_VALUE));
        return optLockValueParameter;
    }

    private List<N2oObject.Parameter> createDynamicParams(Structure structure) {

        if (isEmptyStructure(structure)) {
            return emptyList();
        }

        return structure.getAttributes().stream().map(this::createParam).collect(toList());
    }

    private N2oObject.Parameter createParam(Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setMapping("[1].data['" + attribute.getCode() + "']");

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

        Argument refFromIdArgument = new Argument();
        refFromIdArgument.setType(Argument.Type.PRIMITIVE);
        refFromIdArgument.setClassName("java.lang.Integer");
        refFromIdArgument.setName("referrerVersionId");

        Argument rowSystemIdArgument = new Argument();
        rowSystemIdArgument.setType(Argument.Type.PRIMITIVE);
        rowSystemIdArgument.setClassName("java.lang.Long");
        rowSystemIdArgument.setName("rowSystemId");

        dataProvider.setArguments(new Argument[]{ refFromIdArgument, rowSystemIdArgument });

        N2oConstraint constraint = new N2oConstraint();
        constraint.setId(CONFLICT_VALIDATION_NAME);
        constraint.setSeverity(SeverityType.warning);
        constraint.setServerMoment(N2oValidation.ServerMoment.beforeQuery);
        constraint.setResult("#this == null");
        constraint.setMessage('{' + CONFLICT_TEXT_RESULT + '}');
        constraint.setN2oInvocation(dataProvider);

        N2oObject.Parameter versionIdParam = new N2oObject.Parameter(N2oObject.Parameter.Type.in, DataRecordConstants.FIELD_VERSION_ID, "[0]");
        versionIdParam.setDefaultValue(versionId.toString());
        versionIdParam.setDomain(N2oDomain.INTEGER);

        N2oObject.Parameter rowSysRecordIdParam = new N2oObject.Parameter(N2oObject.Parameter.Type.in, DataRecordConstants.FIELD_SYS_RECORD_ID, "[1]");
        rowSysRecordIdParam.setDomain(N2oDomain.LONG);
        constraint.setInParameters(new N2oObject.Parameter[]{ versionIdParam, rowSysRecordIdParam });

        N2oObject.Parameter conflictTextParam = new N2oObject.Parameter(N2oObject.Parameter.Type.out, CONFLICT_TEXT_RESULT, "(#this)");
        conflictTextParam.setDomain(N2oDomain.STRING);
        N2oObject.Parameter[] outParams = new N2oObject.Parameter[]{ conflictTextParam };
        constraint.setOutParameters(outParams);

        N2oObject.Operation.Validations validations = new N2oObject.Operation.Validations();
        validations.setInlineValidations(new N2oValidation[]{ constraint });
        operation.setValidations(validations);
    }
}

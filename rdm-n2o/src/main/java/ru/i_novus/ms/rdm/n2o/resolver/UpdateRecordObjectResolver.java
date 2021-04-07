package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.exception.SeverityType;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.metadata.global.dao.object.field.ObjectSimpleField;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oConstraint;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oValidation;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.service.UpdateRecordController;

import java.util.List;

import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_SYSTEM_ID;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_VERSION_ID;

@Component
public class UpdateRecordObjectResolver extends DefaultRecordObjectResolver {

    private static final String CONFLICT_VALIDATION_NAME = "checkDataConflicts";
    private static final String CONFLICT_VALIDATION_CLASS_NAME = UpdateRecordController.class.getName();
    private static final String CONFLICT_VALIDATION_CLASS_METHOD = "getDataConflicts";
    private static final String CONFLICT_TEXT_RESULT = "conflictText";

    @Override
    public boolean isSatisfied(String dataAction) {
        return DataRecordConstants.DATA_ACTION_UPDATE.equals(dataAction);
    }

    @Override
    public N2oObject.Operation createOperation(DataRecordRequest request) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("update");
        operation.setInvocation(createInvocation());

        addDataConflictValidation(request.getVersionId(), operation);

        return operation;
    }

    @Override
    public List<AbstractParameter> createRegularParams(DataRecordRequest request) {

        return List.of(
                createVersionIdParameter(request.getVersionId()),
                createOptLockValueParameter(),
                createSystemIdParameter(getRecordMappingIndex(request))
        );
    }

    private AbstractParameter createSystemIdParameter(int index) {

        ObjectSimpleField parameter = new ObjectSimpleField();
        parameter.setId(FIELD_SYSTEM_ID);
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setMapping(String.format("[%d].systemId", index));
        return parameter;
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

        ObjectSimpleField versionIdParam = createObjectSimpleField(FIELD_VERSION_ID, "[0]");
        versionIdParam.setDefaultValue(versionId.toString());
        versionIdParam.setDomain(N2oDomain.INTEGER);

        ObjectSimpleField idParam = createObjectSimpleField(FIELD_SYSTEM_ID, "[1]");
        idParam.setDomain(N2oDomain.LONG);

        constraint.setInFields(new AbstractParameter[]{ versionIdParam, idParam });

        ObjectSimpleField conflictTextParam = createObjectSimpleField(CONFLICT_TEXT_RESULT, "(#this)");
        conflictTextParam.setDomain(N2oDomain.STRING);

        constraint.setOutFields(new ObjectSimpleField[]{ conflictTextParam });

        N2oObject.Operation.Validations validations = new N2oObject.Operation.Validations();
        validations.setInlineValidations(new N2oValidation[]{ constraint });
        operation.setValidations(validations);
    }

    private ObjectSimpleField createObjectSimpleField(String param, String mapping) {

        ObjectSimpleField field = new ObjectSimpleField();
        field.setParam(param);
        field.setMapping(mapping);

        return field;
    }
}

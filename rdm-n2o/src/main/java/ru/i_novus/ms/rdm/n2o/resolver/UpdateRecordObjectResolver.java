package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.exception.SeverityTypeEnum;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.metadata.global.dao.object.field.ObjectSimpleField;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oConstraintValidation;
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

        final N2oObject.Operation operation = new N2oObject.Operation();
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
                createSystemIdParameter()
        );
    }

    private AbstractParameter createSystemIdParameter() {

        final ObjectSimpleField parameter = createParamField(FIELD_SYSTEM_ID, "['row'].systemId");
        parameter.setDomain(N2oDomain.INTEGER);

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

        final N2oJavaDataProvider dataProvider = new N2oJavaDataProvider();
        dataProvider.setClassName(CONFLICT_VALIDATION_CLASS_NAME);
        dataProvider.setMethod(CONFLICT_VALIDATION_CLASS_METHOD);
        dataProvider.setSpringProvider(new SpringProvider());

        final Argument versionIdArgument = createPrimitiveArgument("versionId", Integer.class.getName());
        final Argument idArgument = createPrimitiveArgument("id", Long.class.getName());

        dataProvider.setArguments(new Argument[] {versionIdArgument, idArgument});

        final N2oConstraintValidation constraint = new N2oConstraintValidation();
        constraint.setId(CONFLICT_VALIDATION_NAME);
        constraint.setSeverity(SeverityTypeEnum.WARNING);
        constraint.setServerMoment(N2oValidation.ServerMomentEnum.BEFORE_QUERY);
        constraint.setResult("#this == null");
        constraint.setMessage('{' + CONFLICT_TEXT_RESULT + '}');
        constraint.setN2oInvocation(dataProvider);

        final ObjectSimpleField versionIdParam = createParamField(FIELD_VERSION_ID, "[0]");
        versionIdParam.setDefaultValue(versionId.toString());
        versionIdParam.setDomain(N2oDomain.INTEGER);

        final ObjectSimpleField idParam = createParamField(FIELD_SYSTEM_ID, "[1]");
        idParam.setDomain(N2oDomain.LONG);

        constraint.setInFields(new AbstractParameter[] {versionIdParam, idParam});

        final ObjectSimpleField conflictTextParam = createParamField(CONFLICT_TEXT_RESULT, "(#this)");
        conflictTextParam.setDomain(N2oDomain.STRING);

        constraint.setOutFields(new ObjectSimpleField[] {conflictTextParam});

        final N2oObject.Operation.Validations validations = new N2oObject.Operation.Validations();
        validations.setInlineValidations(new N2oValidation[] {constraint});

        operation.setValidations(validations);
    }
}

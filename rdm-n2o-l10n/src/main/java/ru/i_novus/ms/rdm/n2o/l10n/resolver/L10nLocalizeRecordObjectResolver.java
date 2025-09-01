package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.metadata.global.dao.object.field.ObjectSimpleField;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordObjectResolver;
import ru.i_novus.ms.rdm.n2o.l10n.service.L10nLocalizeVersionController;

import java.util.List;

import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.DATA_ACTION_LOCALIZE;

@Component
public class L10nLocalizeRecordObjectResolver implements DataRecordObjectResolver {

    private static final String CONTROLLER_CLASS_NAME = L10nLocalizeVersionController.class.getName();
    private static final String CONTROLLER_CLASS_METHOD = "localizeDataRecord";

    @Override
    public boolean isSatisfied(String dataAction) {
        return DATA_ACTION_LOCALIZE.equals(dataAction);
    }

    @Override
    public N2oObject.Operation createOperation(DataRecordRequest request) {

        final N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("localize");
        operation.setInvocation(createInvocation());

        return operation;
    }

    protected N2oJavaDataProvider createInvocation() {

        final N2oJavaDataProvider invocation = new N2oJavaDataProvider();
        invocation.setClassName(CONTROLLER_CLASS_NAME);
        invocation.setMethod(CONTROLLER_CLASS_METHOD);
        invocation.setSpringProvider(new SpringProvider());
        invocation.setArguments(getArguments());

        return invocation;
    }

    protected Argument[] getArguments() {

        final Argument versionIdArgument = createPrimitiveArgument("versionId", Integer.class.getName());
        final Argument optLockValueArgument = createPrimitiveArgument("optLockValue", Integer.class.getName());
        final Argument localeCodeArgument = createPrimitiveArgument("localeCode", String.class.getName());

        final Argument rowArgument = new Argument();
        rowArgument.setType(Argument.TypeEnum.CLASS);
        rowArgument.setName("row");
        rowArgument.setClassName(Row.class.getName());

        return new Argument[] {versionIdArgument, optLockValueArgument, localeCodeArgument, rowArgument};
    }

    protected Argument createPrimitiveArgument(String name, String className) {

        final Argument argument = new Argument();
        argument.setName(name);
        argument.setClassName(className);
        argument.setType(Argument.TypeEnum.PRIMITIVE);

        return argument;
    }

    @Override
    public List<AbstractParameter> createRegularParams(DataRecordRequest request) {

        return List.of(
                createVersionIdParameter(request.getVersionId()),
                createOptLockValueParameter(),
                createLocaleCodeParameter(),
                createSystemIdParameter()
        );
    }

    protected AbstractParameter createVersionIdParameter(Integer versionId) {

        final ObjectSimpleField parameter = createParamField(FIELD_VERSION_ID, "['versionId']");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(String.valueOf(versionId));

        return parameter;
    }

    protected AbstractParameter createOptLockValueParameter() {

        final ObjectSimpleField optLockValueParameter = createParamField(FIELD_OPT_LOCK_VALUE, "['optLockValue']");
        optLockValueParameter.setDomain(N2oDomain.INTEGER);
        optLockValueParameter.setDefaultValue(DEFAULT_OPT_LOCK_VALUE);

        return optLockValueParameter;
    }

    private AbstractParameter createLocaleCodeParameter() {

        final ObjectSimpleField localeCodeParameter = createParamField(FIELD_LOCALE_CODE, "['localeCode']");
        localeCodeParameter.setDomain(N2oDomain.STRING);
        localeCodeParameter.setDefaultValue(DEFAULT_LOCALE_CODE);

        return localeCodeParameter;
    }

    private AbstractParameter createSystemIdParameter() {

        final ObjectSimpleField parameter = createParamField(FIELD_SYSTEM_ID, "['row'].systemId");
        parameter.setDomain(N2oDomain.INTEGER);

        return parameter;
    }

    protected ObjectSimpleField createParamField(String id, String mapping) {

        final ObjectSimpleField field = new ObjectSimpleField();
        field.setId(id);
        field.setMapping(mapping);

        return field;
    }
}

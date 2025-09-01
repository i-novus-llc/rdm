package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.field.ObjectSimpleField;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordObjectResolver;
import ru.i_novus.ms.rdm.n2o.service.CreateDraftController;

import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;

public abstract class DefaultRecordObjectResolver implements DataRecordObjectResolver {

    private static final String CONTROLLER_CLASS_NAME = CreateDraftController.class.getName();
    private static final String CONTROLLER_CLASS_METHOD = "updateDataRecord";

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

        final Argument rowArgument = new Argument();
        rowArgument.setType(Argument.TypeEnum.CLASS);
        rowArgument.setName("row");
        rowArgument.setClassName(Row.class.getName());

        return new Argument[] {versionIdArgument, optLockValueArgument, rowArgument};
    }

    protected Argument createPrimitiveArgument(String name, String className) {

        final Argument argument = new Argument();
        argument.setName(name);
        argument.setClassName(className);
        argument.setType(Argument.TypeEnum.PRIMITIVE);

        return argument;
    }

    protected AbstractParameter createVersionIdParameter(Integer versionId) {

        final ObjectSimpleField parameter = createParamField(FIELD_VERSION_ID, "['versionId']");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(String.valueOf(versionId));

        return parameter;
    }

    protected AbstractParameter createOptLockValueParameter() {

        final ObjectSimpleField parameter = createParamField(FIELD_OPT_LOCK_VALUE, "['optLockValue']");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(DEFAULT_OPT_LOCK_VALUE);

        return parameter;
    }

    protected ObjectSimpleField createParamField(String id, String mapping) {

        final ObjectSimpleField field = new ObjectSimpleField();
        field.setId(id);
        field.setMapping(mapping);

        return field;
    }
}

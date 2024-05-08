package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
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

        final Argument versionIdArgument = new Argument();
        versionIdArgument.setType(Argument.Type.PRIMITIVE);
        versionIdArgument.setName("versionId");
        versionIdArgument.setClassName(Integer.class.getName());

        final Argument optLockValueArgument = new Argument();
        optLockValueArgument.setType(Argument.Type.PRIMITIVE);
        optLockValueArgument.setName("optLockValue");
        optLockValueArgument.setClassName(Integer.class.getName());

        final Argument rowArgument = new Argument();
        rowArgument.setType(Argument.Type.CLASS);
        rowArgument.setName("row");
        rowArgument.setClassName(Row.class.getName());

        return new Argument[] {versionIdArgument, optLockValueArgument, rowArgument};
    }

    protected AbstractParameter createVersionIdParameter(Integer versionId) {

        final ObjectSimpleField parameter = new ObjectSimpleField();
        parameter.setId(FIELD_VERSION_ID);
        parameter.setMapping("['versionId']");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(String.valueOf(versionId));

        return parameter;
    }

    protected AbstractParameter createOptLockValueParameter() {

        final ObjectSimpleField parameter = new ObjectSimpleField();
        parameter.setId(FIELD_OPT_LOCK_VALUE);
        parameter.setMapping("['optLockValue']");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(String.valueOf(DEFAULT_OPT_LOCK_VALUE));

        return parameter;
    }
}

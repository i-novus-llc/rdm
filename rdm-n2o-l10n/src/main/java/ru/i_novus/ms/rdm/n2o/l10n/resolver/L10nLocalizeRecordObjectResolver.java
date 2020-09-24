package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
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

    private static final int ROW_ARGUMENT_INDEX = 3;

    private static final String CONTROLLER_CLASS_NAME = L10nLocalizeVersionController.class.getName();
    private static final String CONTROLLER_CLASS_METHOD = "localizeDataRecord";

    @Override
    public boolean isSatisfied(String dataAction) {
        return DATA_ACTION_LOCALIZE.equals(dataAction);
    }

    @Override
    public N2oObject.Operation createOperation(DataRecordRequest request) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("localize");
        operation.setInvocation(createInvocation());

        return operation;
    }

    @Override
    public List<N2oObject.Parameter> createRegularParams(DataRecordRequest request) {

        return List.of(
                createVersionIdParameter(request.getVersionId()),
                createOptLockValueParameter(),
                createLocaleCodeParameter(),
                createSystemIdParameter(getRecordMappingIndex(request))
        );
    }

    @Override
    public int getRecordMappingIndex(DataRecordRequest request) {
        return ROW_ARGUMENT_INDEX;
    }

    protected N2oJavaDataProvider createInvocation() {

        N2oJavaDataProvider invocation = new N2oJavaDataProvider();
        invocation.setClassName(CONTROLLER_CLASS_NAME);
        invocation.setMethod(CONTROLLER_CLASS_METHOD);
        invocation.setSpringProvider(new SpringProvider());
        invocation.setArguments(getArguments());

        return invocation;
    }

    protected Argument[] getArguments() {

        Argument versionIdArgument = new Argument();
        versionIdArgument.setType(Argument.Type.PRIMITIVE);
        versionIdArgument.setName("versionId");
        versionIdArgument.setClassName(Integer.class.getName());

        Argument optLockValueArgument = new Argument();
        optLockValueArgument.setType(Argument.Type.PRIMITIVE);
        optLockValueArgument.setName("optLockValue");
        optLockValueArgument.setClassName(Integer.class.getName());

        Argument localeCodeArgument = new Argument();
        localeCodeArgument.setType(Argument.Type.PRIMITIVE);
        localeCodeArgument.setName("localeCode");
        localeCodeArgument.setClassName(String.class.getName());

        Argument rowArgument = new Argument();
        rowArgument.setType(Argument.Type.CLASS);
        rowArgument.setName("row");
        rowArgument.setClassName(Row.class.getName());

        return new Argument[]{ versionIdArgument, optLockValueArgument, localeCodeArgument, rowArgument };
    }

    protected N2oObject.Parameter createVersionIdParameter(Integer versionId) {

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId(FIELD_VERSION_ID);
        parameter.setMapping("[0]");
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setDefaultValue(String.valueOf(versionId));
        return parameter;
    }

    protected N2oObject.Parameter createOptLockValueParameter() {

        N2oObject.Parameter optLockValueParameter = new N2oObject.Parameter();
        optLockValueParameter.setId(FIELD_OPT_LOCK_VALUE);
        optLockValueParameter.setMapping("[1]");
        optLockValueParameter.setDomain(N2oDomain.INTEGER);
        optLockValueParameter.setDefaultValue(String.valueOf(DEFAULT_OPT_LOCK_VALUE));
        return optLockValueParameter;
    }

    private N2oObject.Parameter createLocaleCodeParameter() {

        N2oObject.Parameter localeCodeParameter = new N2oObject.Parameter();
        localeCodeParameter.setId(FIELD_LOCALE_CODE);
        localeCodeParameter.setMapping("[2]");
        localeCodeParameter.setDomain(N2oDomain.STRING);
        localeCodeParameter.setDefaultValue(DEFAULT_LOCALE_CODE);
        return localeCodeParameter;
    }

    private N2oObject.Parameter createSystemIdParameter(int index) {

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId(FIELD_SYSTEM_ID);
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setMapping(String.format("[%d].systemId", index));
        return parameter;
    }
}

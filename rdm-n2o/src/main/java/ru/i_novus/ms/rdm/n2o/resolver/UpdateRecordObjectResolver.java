package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.FIELD_SYSTEM_ID;

@Component
public class UpdateRecordObjectResolver extends DefaultRecordObjectResolver {

    @Override
    public boolean isSatisfied(String dataAction) {
        return DataRecordConstants.DATA_ACTION_UPDATE.equals(dataAction);
    }

    @Override
    public N2oObject.Operation createOperation(DataRecordRequest request) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("update");
        operation.setInvocation(createInvocation());

        return operation;
    }

    @Override
    public List<N2oObject.Parameter> createRegularParams(DataRecordRequest request) {

        return List.of(
                createVersionIdParameter(request.getVersionId()),
                createOptLockValueParameter(),
                createSystemIdParameter(getRecordMappingIndex(request))
        );
    }

    private N2oObject.Parameter createSystemIdParameter(int index) {

        N2oObject.Parameter parameter = new N2oObject.Parameter();
        parameter.setId(FIELD_SYSTEM_ID);
        parameter.setDomain(N2oDomain.INTEGER);
        parameter.setMapping(String.format("[%d].systemId", index));
        return parameter;
    }
}

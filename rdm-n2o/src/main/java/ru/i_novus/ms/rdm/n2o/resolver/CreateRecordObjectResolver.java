package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;

import java.util.List;

@Component
public class CreateRecordObjectResolver extends DefaultRecordObjectResolver {

    @Override
    public boolean isSatisfied(String dataAction) {
        return DataRecordConstants.DATA_ACTION_CREATE.equals(dataAction);
    }

    @Override
    public N2oObject.Operation createOperation(DataRecordRequest request) {

        N2oObject.Operation operation = new N2oObject.Operation();
        operation.setId("create");
        operation.setInvocation(createInvocation());

        return operation;
    }

    @Override
    public List<AbstractParameter> createRegularParams(DataRecordRequest request) {

        return List.of(
                createVersionIdParameter(request.getVersionId()),
                createOptLockValueParameter()
        );
    }
}

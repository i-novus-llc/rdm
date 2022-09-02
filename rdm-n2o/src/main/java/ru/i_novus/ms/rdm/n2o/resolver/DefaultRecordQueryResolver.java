package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.global.dao.query.field.QuerySimpleField;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordQueryResolver;

import java.util.List;

import static java.util.Collections.emptyList;

@Component
public class DefaultRecordQueryResolver implements DataRecordQueryResolver {

    @Override
    public boolean isSatisfied(String dataAction) {
        return DataRecordConstants.getDataActions().contains(dataAction);
    }

    @Override
    public List<QuerySimpleField> createRegularFields(DataRecordRequest request) {
        return emptyList();
    }
}

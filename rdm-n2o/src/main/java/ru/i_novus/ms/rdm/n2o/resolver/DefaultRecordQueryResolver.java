package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.global.dao.N2oQuery;
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
    public List<N2oQuery.Field> createRegularFields(DataRecordRequest request) {
        return emptyList();
    }
}

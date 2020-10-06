package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.SourceComponent;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordPageResolver;

import java.util.List;

import static java.util.Collections.emptyList;

@Component
public class DefaultRecordPageResolver implements DataRecordPageResolver {

    @Override
    public boolean isSatisfied(String dataAction) {
        return DataRecordConstants.getDataActions().contains(dataAction);
    }

    @Override
    public List<SourceComponent> createRegularFields(DataRecordRequest request) {
        return emptyList();
    }

    @Override
    public void processDynamicFields(DataRecordRequest request,
                                     List<SourceComponent> list) {
        // Nothing to do.
    }
}

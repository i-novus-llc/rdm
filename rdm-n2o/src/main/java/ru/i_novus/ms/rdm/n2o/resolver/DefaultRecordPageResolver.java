package ru.i_novus.ms.rdm.n2o.resolver;

import net.n2oapp.framework.api.metadata.control.N2oField;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordPageResolver;

import java.util.List;

import static java.util.Collections.emptyList;
import static ru.i_novus.ms.rdm.n2o.constant.DataRecordConstants.DATA_ACTION_CREATE;
import static ru.i_novus.ms.rdm.n2o.constant.DataRecordConstants.DATA_ACTION_UPDATE;

@Component
public class DefaultRecordPageResolver implements DataRecordPageResolver {

    private static final List<String> DEFAULT_DATA_ACTIONS = List.of(DATA_ACTION_CREATE, DATA_ACTION_UPDATE);

    @Override
    public boolean isSatisfied(String dataAction) {
        return DEFAULT_DATA_ACTIONS.contains(dataAction);
    }

    @Override
    public List<N2oField> createRegularFields(Integer versionId, Structure structure, String dataAction) {
        return emptyList();
    }

    @Override
    public void processDynamicFields(Integer versionId, Structure structure, String dataAction, List<N2oField> list) {
        // Nothing to do.
    }
}

package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import net.n2oapp.framework.api.metadata.global.dao.query.N2oQuery;
import net.n2oapp.framework.api.metadata.global.dao.query.field.QuerySimpleField;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordQueryResolver;

import java.util.List;

import static java.util.Collections.emptyList;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.DATA_ACTION_LOCALIZE;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.FIELD_LOCALE_NAME;

@Component
public class L10nLocalizeRecordQueryResolver implements DataRecordQueryResolver {

    @Override
    public boolean isSatisfied(String dataAction) {
        return DATA_ACTION_LOCALIZE.equals(dataAction);
    }

    @Override
    public List<QuerySimpleField> createRegularFields(DataRecordRequest request) {

        return List.of(createLocaleNameField());
    }

    private QuerySimpleField createLocaleNameField() {

        QuerySimpleField field = new QuerySimpleField();
        field.setId(FIELD_LOCALE_NAME);

        return field;
    }

    @Override
    public List<N2oQuery.Filter> createRegularFilters(DataRecordRequest request) {
        return emptyList();
    }
}

package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import net.n2oapp.framework.api.metadata.SourceComponent;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.plain.N2oCheckbox;
import net.n2oapp.framework.api.metadata.control.plain.N2oOutputText;
import net.n2oapp.framework.api.metadata.global.view.fieldset.N2oFieldsetRow;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordPageResolver;

import java.util.List;

import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.DATA_ACTION_LOCALIZE;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.FIELD_LOCALE_NAME;

@Component
public class L10nLocalizeRecordPageResolver implements DataRecordPageResolver {

    @Override
    public boolean isSatisfied(String dataAction) {
        return DATA_ACTION_LOCALIZE.contains(dataAction);
    }

    @Override
    public List<SourceComponent> createRegularFields(Integer versionId, Structure structure, String dataAction) {

        N2oFieldsetRow row = new N2oFieldsetRow();
        row.setItems(createLocalizeFields());

        return singletonList(row);
    }

    @Override
    public void processDynamicFields(Integer versionId, Structure structure, String dataAction,
                                     List<SourceComponent> list) {
        // Nothing to do.
    }

    private SourceComponent[] createLocalizeFields() {

        return new SourceComponent[] { createLocaleCodeField(), createHideUnlocalizableField() };
    }

    private N2oField createLocaleCodeField() {

        N2oOutputText n2oField = new N2oOutputText();
        n2oField.setId(FIELD_LOCALE_NAME);
        n2oField.setDomain(N2oDomain.STRING);
        n2oField.setNoLabelBlock(Boolean.TRUE);
        
        return n2oField;
    }

    private N2oField createHideUnlocalizableField() {

        N2oCheckbox n2oField = new N2oCheckbox();
        n2oField.setId("hideUnlocalizable");
        n2oField.setNoLabelBlock(Boolean.TRUE);
        n2oField.setLabel("Скрыть поля, не требующие перевода");

        return n2oField;
    }
}

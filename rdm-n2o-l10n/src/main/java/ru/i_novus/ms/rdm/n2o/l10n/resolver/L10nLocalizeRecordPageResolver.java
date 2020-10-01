package ru.i_novus.ms.rdm.n2o.l10n.resolver;

import net.n2oapp.framework.api.metadata.SourceComponent;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.N2oStandardField;
import net.n2oapp.framework.api.metadata.control.plain.CheckboxDefaultValueEnum;
import net.n2oapp.framework.api.metadata.control.plain.N2oCheckbox;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.global.view.fieldset.N2oFieldsetRow;
import net.n2oapp.platform.i18n.Messages;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordPageResolver;
import ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils;

import java.util.List;

import static java.util.Collections.singletonList;
import static ru.i_novus.ms.rdm.n2o.l10n.constant.L10nRecordConstants.*;

@Component
public class L10nLocalizeRecordPageResolver implements DataRecordPageResolver {

    private static final String LABEL_HIDE_UNLOCALIZABLE = "label.hide.unlocalizable";

    @Autowired
    private Messages messages;

    @Override
    public boolean isSatisfied(String dataAction) {
        return DATA_ACTION_LOCALIZE.equals(dataAction);
    }

    @Override
    public List<SourceComponent> createRegularFields(DataRecordRequest request) {

        N2oFieldsetRow row = new N2oFieldsetRow();
        row.setItems(createLocalizeFields());

        return singletonList(row);
    }

    private SourceComponent[] createLocalizeFields() {

        return new SourceComponent[]{
                createLocaleCodeField(),
                createHideUnlocalizableField()
        };
    }

    private N2oField createLocaleCodeField() {

        N2oInputText n2oField = new N2oInputText();
        n2oField.setId(FIELD_LOCALE_NAME);
        n2oField.setDomain(N2oDomain.STRING);
        n2oField.setNoLabelBlock(Boolean.TRUE);
        n2oField.setEnabled(Boolean.FALSE);

        return n2oField;
    }

    private N2oField createHideUnlocalizableField() {

        N2oCheckbox n2oField = new N2oCheckbox();
        n2oField.setId(FIELD_HIDE_UNLOCALIZABLE);
        n2oField.setNoLabelBlock(Boolean.TRUE);
        n2oField.setUnchecked(CheckboxDefaultValueEnum.FALSE);
        n2oField.setLabel(messages.getMessage(LABEL_HIDE_UNLOCALIZABLE));

        return n2oField;
    }

    @Override
    public void processDynamicFields(DataRecordRequest request, List<SourceComponent> list) {

        final Structure structure = request.getStructure();
        if (structure.getAttributes().stream().noneMatch(Structure.Attribute::isLocalizable))
            return;

        list.stream()
                .filter(item -> isUnlocalizable(item, structure))
                .forEach(this::processUnlocalizable);
    }

    /** Проверка поля формы на соответствие непереводимому атрибуту. */
    private boolean isUnlocalizable(SourceComponent component, Structure structure) {

        if (!(component instanceof N2oStandardField))
            return false;

        N2oStandardField field = (N2oStandardField) component;
        if (!DataRecordUtils.hasPrefix(field.getId()))
            return false;

        String attributeCode = DataRecordUtils.deletePrefix(field.getId());
        Structure.Attribute attribute = structure.getAttribute(attributeCode);
        return attribute != null && !attribute.isLocalizable();
    }

    private void processUnlocalizable(SourceComponent component) {

        N2oStandardField field = (N2oStandardField) component;
        field.setEnabled(Boolean.FALSE);

        N2oField.VisibilityDependency dependency = new N2oField.VisibilityDependency();
        dependency.setOn(new String[]{FIELD_HIDE_UNLOCALIZABLE});
        dependency.setValue("!" + FIELD_HIDE_UNLOCALIZABLE);
        dependency.setApplyOnInit(Boolean.FALSE);

        field.addDependency(dependency);
    }
}

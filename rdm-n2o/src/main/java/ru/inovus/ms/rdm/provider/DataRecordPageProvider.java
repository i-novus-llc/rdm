package ru.inovus.ms.rdm.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.N2oStandardField;
import net.n2oapp.framework.api.metadata.control.list.N2oInputSelect;
import net.n2oapp.framework.api.metadata.control.plain.N2oCheckbox;
import net.n2oapp.framework.api.metadata.control.plain.N2oDatePicker;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.global.view.page.N2oPage;
import net.n2oapp.framework.api.metadata.global.view.page.N2oSimplePage;
import net.n2oapp.framework.api.metadata.global.view.widget.N2oForm;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Collection;
import java.util.List;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.RdmUiUtil.addPrefix;

@Service
public class DataRecordPageProvider implements DynamicMetadataProvider {

    static final String FORM_PROVIDER_ID = "dataRecordPage";

    @Autowired
    private VersionService versionService;

    /**
     * @return Код провайдера
     */
    @Override
    public String getCode() {
        return FORM_PROVIDER_ID;
    }

    /**
     * @param context Параметры провайдера (ID версии)
     */
    @Override
    public List<? extends SourceMetadata> read(String context) {

        Integer versionId = Integer.parseInt(context);
        Structure structure = versionService.getStructure(versionId);

        N2oSimplePage page = new N2oSimplePage();
        N2oForm form = createForm(versionId, structure);
        page.setWidget(form);
        page.setName("Редактирование записи");
        page.setId(FORM_PROVIDER_ID + "?" + context);
        return singletonList(page);
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oPage.class);
    }

    private N2oForm createForm(Integer versionId, Structure structure) {
        N2oForm n2oForm = new N2oForm();
        n2oForm.setItems(createFields(structure));
        n2oForm.setQueryId(DataRecordQueryProvider.QUERY_PROVIDER_ID + "?" + versionId);
        n2oForm.setObjectId(DataRecordObjectProvider.OBJECT_PROVIDER_ID + "?" + versionId);
        return n2oForm;
    }

    private N2oField[] createFields(Structure structure) {
        return structure.getAttributes().stream()
                .map(this::toN2oField)
                .toArray(N2oField[]::new);
    }

    private N2oStandardField toN2oField(Structure.Attribute attribute) {
        N2oStandardField n2oField;
        switch (attribute.getType()) {
            case BOOLEAN:
                n2oField = new N2oCheckbox();
                break;
            case DATE:
                N2oDatePicker n2oField1 = new N2oDatePicker();
                n2oField1.setDateFormat("DD.MM.YYYY");
                n2oField = n2oField1;

                break;
            case INTEGER:
                N2oInputText integerField = new N2oInputText();
                integerField.setDomain("integer");
                integerField.setStep("1");
                n2oField = integerField;
                break;
            case FLOAT:
                N2oInputText floatField = new N2oInputText();
                floatField.setDomain("numeric");
                floatField.setStep("0.0001");
                n2oField = floatField;
                break;
            case REFERENCE:
                N2oInputSelect n2oInputSelect = new N2oInputSelect();
                n2oInputSelect.setQueryId("reference");
                n2oInputSelect.setValueFieldId("value");
                n2oInputSelect.setLabelFieldId("displayValue");
                n2oField = n2oInputSelect;
                break;
            default:
                n2oField = new N2oInputText();

        }
        n2oField.setId(addPrefix(attribute.getCode()));
        n2oField.setLabel(attribute.getName());
        return n2oField;
    }
}

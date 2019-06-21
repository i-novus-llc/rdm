package ru.inovus.ms.rdm.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.N2oStandardField;
import net.n2oapp.framework.api.metadata.control.list.N2oInputSelect;
import net.n2oapp.framework.api.metadata.control.plain.N2oCheckbox;
import net.n2oapp.framework.api.metadata.control.plain.N2oDatePicker;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.global.dao.N2oPreFilter;
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
import java.util.Map;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.RdmUiUtil.addPrefix;

@Service
public class DataRecordPageProvider implements DynamicMetadataProvider {

    private static final String FORM_PROVIDER_ID = "dataRecordPage";
    private static final Map<String, String> pageNames = Map.of(
            "create", "Добавление новой записи",
            "edit", "Редактирование записи"
    );

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
     * @param context Параметры провайдера (ID версии и тип действия) в формате versionId_pageType,
     *                где pageType - create (Создание записи) или edit (Редактирование записи)
     */
    @Override
    public List<? extends SourceMetadata> read(String context) {

        String[] params = context.split("_");

        Integer versionId = Integer.parseInt(params[0]);
        Structure structure = versionService.getStructure(versionId);

        N2oSimplePage page = new N2oSimplePage();
        N2oForm form = createForm(versionId, structure);
        page.setWidget(form);
        page.setName(pageNames.get(params[1]));
        page.setId(FORM_PROVIDER_ID + "?" + context);
        return singletonList(page);
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oPage.class);
    }

    private N2oForm createForm(Integer versionId, Structure structure) {
        N2oForm n2oForm = new N2oForm();
        n2oForm.setItems(createFields(versionId, structure));
        n2oForm.setQueryId(DataRecordQueryProvider.QUERY_PROVIDER_ID + "?" + versionId);
        n2oForm.setObjectId(DataRecordObjectProvider.OBJECT_PROVIDER_ID + "?" + versionId);
        return n2oForm;
    }

    private N2oField[] createFields(Integer versionId, Structure structure) {
        return structure.getAttributes().stream()
                .map(attribute -> toN2oField(versionId, attribute))
                .toArray(N2oField[]::new);
    }

    private N2oStandardField toN2oField(Integer versionId, Structure.Attribute attribute) {
        N2oStandardField n2oField;

        switch (attribute.getType()) {
            case INTEGER:
                N2oInputText integerField = new N2oInputText();
                integerField.setDomain(N2oDomain.INTEGER);
                integerField.setStep("1");
                n2oField = integerField;
                break;

            case FLOAT:
                N2oInputText floatField = new N2oInputText();
                floatField.setDomain(N2oDomain.FLOAT);
                floatField.setStep("0.0001");
                n2oField = floatField;
                break;

            case DATE:
                N2oDatePicker dateField = new N2oDatePicker();
                dateField.setDateFormat("DD.MM.YYYY");
                n2oField = dateField;
                break;

            case BOOLEAN:
                n2oField = new N2oCheckbox();
                break;

            case REFERENCE:
                N2oInputSelect referenceField = new N2oInputSelect();
                referenceField.setQueryId("reference");
                //NB: value-field-id is deprecated:
                referenceField.setValueFieldId("value");
                referenceField.setLabelFieldId("displayValue");

                N2oPreFilter versionFilter = new N2oPreFilter();
                versionFilter.setType(FilterType.eq);
                versionFilter.setFieldId("versionId");
                versionFilter.setValueAttr(versionId.toString());

                N2oPreFilter referenceFilter = new N2oPreFilter();
                referenceFilter.setType(FilterType.eq);
                referenceFilter.setFieldId("reference");
                referenceFilter.setValueAttr(attribute.getCode());

                referenceField.setPreFilters(new N2oPreFilter[]{versionFilter, referenceFilter});

                n2oField = referenceField;
                break;

            default:
                n2oField = new N2oInputText();
        }

        n2oField.setId(addPrefix(attribute.getCode()));
        n2oField.setLabel(attribute.getName());

        return n2oField;
    }
}

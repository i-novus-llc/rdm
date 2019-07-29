package ru.inovus.ms.rdm.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.exception.SeverityType;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.N2oStandardField;
import net.n2oapp.framework.api.metadata.control.list.N2oInputSelect;
import net.n2oapp.framework.api.metadata.control.plain.N2oCheckbox;
import net.n2oapp.framework.api.metadata.control.plain.N2oDatePicker;
import net.n2oapp.framework.api.metadata.control.plain.N2oInputText;
import net.n2oapp.framework.api.metadata.dataprovider.N2oJavaDataProvider;
import net.n2oapp.framework.api.metadata.dataprovider.SpringProvider;
import net.n2oapp.framework.api.metadata.global.dao.N2oPreFilter;
import net.n2oapp.framework.api.metadata.global.dao.invocation.model.Argument;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oConstraint;
import net.n2oapp.framework.api.metadata.global.dao.validation.N2oValidation;
import net.n2oapp.framework.api.metadata.global.view.page.N2oPage;
import net.n2oapp.framework.api.metadata.global.view.page.N2oSimplePage;
import net.n2oapp.framework.api.metadata.global.view.widget.N2oForm;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.service.api.ConflictService;
import ru.inovus.ms.rdm.service.api.VersionService;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static ru.inovus.ms.rdm.RdmUiUtil.addPrefix;

@Service
@SuppressWarnings("unused")
public class DataRecordPageProvider implements DynamicMetadataProvider {

    private static final String FORM_PROVIDER_ID = "dataRecordPage";
    private static final Map<String, String> pageNames = Map.of(
            "create", "Добавление новой записи",
            "edit", "Редактирование записи"
    );

    private static final String CONFLICT_TYPE_MAPPING = "\"UPDATED\".equals(conflictType.name()) ? \"изменена строка\" : " +
        "\"DELETED\".equals(conflictType.name()) ? \"удалена строка\" : " +
        "\"изменена структура\""; // ALTERED.equals(conflictType.name())

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
    @SuppressWarnings("unchecked")
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
                // NB: value-field-id is deprecated:
                referenceField.setValueFieldId("value");
                referenceField.setLabelFieldId("displayValue");
                referenceField.setDomain(N2oDomain.STRING);

                N2oPreFilter versionFilter = new N2oPreFilter();
                versionFilter.setType(FilterType.eq);
                versionFilter.setFieldId("versionId");
                versionFilter.setValueAttr(versionId.toString());

                N2oPreFilter referenceFilter = new N2oPreFilter();
                referenceFilter.setType(FilterType.eq);
                referenceFilter.setFieldId("reference");
                referenceFilter.setValueAttr(attribute.getCode());

                referenceField.setPreFilters(new N2oPreFilter[]{versionFilter, referenceFilter});

                N2oValidation validation = createRefValueValidation(attribute.getCode());
                N2oField.Validations validations = new N2oField.Validations();
                validations.setInlineValidations(new N2oValidation[]{validation});
                referenceField.setValidations(validations);

                n2oField = referenceField;
                break;

            default:
                n2oField = new N2oInputText();
        }

        n2oField.setId(addPrefix(attribute.getCode()));
        n2oField.setLabel(attribute.getName());

        return n2oField;
    }

    private N2oValidation createRefValueValidation(String attributeCode) {
        String attributeCodeWithPrefix = addPrefix(attributeCode);

        N2oJavaDataProvider dataProvider = new N2oJavaDataProvider();
        dataProvider.setClassName(ConflictService.class.getName());
        dataProvider.setMethod("findDataConflict");
        dataProvider.setSpringProvider(new SpringProvider());

        Argument refFromIdArgument = new Argument();
        refFromIdArgument.setType(Argument.Type.PRIMITIVE);
        refFromIdArgument.setClassName("java.lang.Integer");
        refFromIdArgument.setName("refFromId");

        Argument refFieldCodeArgument = new Argument();
        refFieldCodeArgument.setType(Argument.Type.PRIMITIVE);
        refFieldCodeArgument.setClassName("java.lang.String");
        refFieldCodeArgument.setName("refFieldCode");

        Argument rowSystemIdArgument = new Argument();
        rowSystemIdArgument.setType(Argument.Type.PRIMITIVE);
        rowSystemIdArgument.setClassName("java.lang.Long");
        rowSystemIdArgument.setName("rowSystemId");

        dataProvider.setArguments(new Argument[] {refFromIdArgument, refFieldCodeArgument, rowSystemIdArgument});

        N2oConstraint constraint = new N2oConstraint();
        constraint.setId("_constraint_validation");
        constraint.setFieldId(attributeCodeWithPrefix);
        constraint.setMessage("В связанном справочнике была {conflictType}");
        constraint.setSeverity(SeverityType.danger);
        constraint.setResult("#this == null");

        N2oObject.Parameter refFromIdParam = new N2oObject.Parameter(N2oObject.Parameter.Type.in, "versionId", "[0]");
        refFromIdParam.setDomain(N2oDomain.INTEGER);
        N2oObject.Parameter refFieldCodeParam = new N2oObject.Parameter(N2oObject.Parameter.Type.in, attributeCode, "[1]");
        refFieldCodeParam.setDefaultValue(attributeCode);
        refFieldCodeParam.setDomain(N2oDomain.STRING);
        N2oObject.Parameter rowSystemIdParam = new N2oObject.Parameter(N2oObject.Parameter.Type.in, "id", "[2]");
        rowSystemIdParam.setDomain(N2oDomain.LONG);

        constraint.setInParameters(new N2oObject.Parameter[]{refFromIdParam, refFieldCodeParam, rowSystemIdParam});

        N2oObject.Parameter conflictTypeParam = new N2oObject.Parameter(N2oObject.Parameter.Type.out, "conflictType", CONFLICT_TYPE_MAPPING);
        conflictTypeParam.setDomain(N2oDomain.STRING);
        N2oObject.Parameter[] outParams = new N2oObject.Parameter[]{conflictTypeParam};
        constraint.setOutParameters(outParams);
        constraint.setServerMoment(N2oValidation.ServerMoment.afterSuccessQuery);
        constraint.setSide("server");

        constraint.setN2oInvocation(dataProvider);

        return constraint;
    }
}

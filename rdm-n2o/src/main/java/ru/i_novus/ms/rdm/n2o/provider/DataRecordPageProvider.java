package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.criteria.filters.FilterType;
import net.n2oapp.framework.api.metadata.SourceComponent;
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
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordPageResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.RdmUiUtil.addPrefix;

/**
 * Провайдер для формирования страницы по отображению данных
 * по создаваемой/изменяемой записи из указанной версии справочника.
 */
@Service
@SuppressWarnings("unused")
public class DataRecordPageProvider extends DataRecordBaseProvider implements DynamicMetadataProvider {

    private static final String PAGE_PROVIDER_ID = "dataRecordPage";

    @Autowired
    private Collection<DataRecordPageResolver> resolvers;

    @Override
    public String getCode() {
        return PAGE_PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        // Метод провайдера отрабатывает также на этапе Transform
        // (@see AbstractActionTransformer.mapSecurity).
        // На этом этапе для {id} не установлено значение.
        if (context.contains("{") || context.contains("}"))
            return singletonList(new N2oSimplePage());

        DataRecordRequest request = toRequest(context);
        return singletonList(createPage(context, request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oPage.class);
    }

    private N2oSimplePage createPage(String context, DataRecordRequest request) {

        N2oSimplePage page = new N2oSimplePage();
        page.setId(PAGE_PROVIDER_ID + "?" + context);

        page.setWidget(createForm(context, request));

        return page;
    }

    private N2oForm createForm(String context, DataRecordRequest request) {

        N2oForm n2oForm = new N2oForm();
        n2oForm.setQueryId(DataRecordQueryProvider.QUERY_PROVIDER_ID + "?" + context);
        n2oForm.setObjectId(DataRecordObjectProvider.OBJECT_PROVIDER_ID + "?" + context);

        n2oForm.setItems(createPageFields(request));

        return n2oForm;
    }

    private SourceComponent[] createPageFields(DataRecordRequest request) {

        if (request.getStructure().isEmpty()) {
            return new N2oField[0];
        }

        return Stream.concat(
                createRegularFields(request).stream(),
                createDynamicFields(request).stream())
                .toArray(SourceComponent[]::new);
    }

    private List<SourceComponent> createRegularFields(DataRecordRequest request) {

        return getSatisfiedResolvers(request.getDataAction())
                .map(resolver -> resolver.createRegularFields(request))
                .flatMap(Collection::stream).collect(toList());
    }

    private List<SourceComponent> createDynamicFields(DataRecordRequest request) {

        Integer versionId = request.getVersionId();
        Structure structure = request.getStructure();

        List<SourceComponent> list = new ArrayList<>();
        for (Structure.Attribute attribute : structure.getAttributes()) {

            N2oStandardField n2oField;
            if (attribute.isReferenceType()) {
                n2oField = createReferenceField(versionId, attribute);

            } else {
                n2oField = createField(attribute);
            }

            if (attribute.hasIsPrimary()) {
                n2oField.setRequired(true);
            }

            list.add(n2oField);
        }

        getSatisfiedResolvers(request.getDataAction()).forEach(resolver ->
                resolver.processDynamicFields(request, list)
        );

        return list;
    }

    private N2oStandardField createField(Structure.Attribute attribute) {

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
                n2oField.setNoLabelBlock(Boolean.TRUE);
                break;

            default:
                n2oField = new N2oInputText();
        }

        n2oField.setId(addPrefix(attribute.getCode()));
        n2oField.setLabel(attribute.getName());

        return n2oField;
    }

    private N2oStandardField createReferenceField(Integer versionId, Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());

        N2oInputSelect referenceField = new N2oInputSelect();
        referenceField.setId(codeWithPrefix);
        referenceField.setLabel(attribute.getName());

        referenceField.setQueryId(REFERENCE_QUERY_ID);
        // NB: value-field-id is deprecated:
        referenceField.setValueFieldId(REFERENCE_VALUE);
        referenceField.setLabelFieldId(REFERENCE_DISPLAY_VALUE);
        referenceField.setDomain(N2oDomain.STRING);

        N2oPreFilter versionFilter = new N2oPreFilter();
        versionFilter.setType(FilterType.eq);
        versionFilter.setFieldId("versionId");
        versionFilter.setValueAttr(versionId.toString());

        N2oPreFilter referenceFilter = new N2oPreFilter();
        referenceFilter.setType(FilterType.eq);
        referenceFilter.setFieldId("reference");
        referenceFilter.setValueAttr(attribute.getCode());

        referenceField.setPreFilters(new N2oPreFilter[]{ versionFilter, referenceFilter });

        return referenceField;
    }

    private Stream<DataRecordPageResolver> getSatisfiedResolvers(String dataAction) {

        if (isEmpty(resolvers))
            return Stream.empty();

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(dataAction));
    }
}

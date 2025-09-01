package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.criteria.filters.FilterTypeEnum;
import net.n2oapp.framework.api.metadata.SourceComponent;
import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.control.N2oField;
import net.n2oapp.framework.api.metadata.control.N2oStandardField;
import net.n2oapp.framework.api.metadata.control.list.N2oInputSelect;
import net.n2oapp.framework.api.metadata.global.dao.N2oPreFilter;
import net.n2oapp.framework.api.metadata.global.view.page.N2oPage;
import net.n2oapp.framework.api.metadata.global.view.page.N2oSimplePage;
import net.n2oapp.framework.api.metadata.global.view.page.datasource.N2oStandardDatasource;
import net.n2oapp.framework.api.metadata.global.view.widget.N2oForm;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordPageResolver;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.*;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

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

        final DataRecordRequest request = toRequest(context);
        return singletonList(createPage(context, request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oPage.class);
    }

    private N2oSimplePage createPage(String context, DataRecordRequest request) {

        final N2oSimplePage page = new N2oSimplePage();
        page.setId(PAGE_PROVIDER_ID + "?" + context);
        page.setWidget(createForm(context, request));

        return page;
    }

    private N2oForm createForm(String context, DataRecordRequest request) {

        final N2oForm form = new N2oForm();
        form.setDatasource(createDatasource(context));
        form.setItems(createPageFields(request));

        return form;
    }

    private N2oStandardDatasource createDatasource(String context) {

        final N2oStandardDatasource datasource = new N2oStandardDatasource();
        datasource.setQueryId(DataRecordQueryProvider.QUERY_PROVIDER_ID + "?" + context);
        datasource.setObjectId(DataRecordObjectProvider.OBJECT_PROVIDER_ID + "?" + context);
        datasource.setFilters(createPreFilters());

        return datasource;
    }

    private N2oPreFilter[] createPreFilters() {

        final N2oPreFilter idFilter = createParamEqualFilter(FIELD_SYSTEM_ID);

        final N2oPreFilter optLockValueFilter = createParamEqualFilter(FIELD_OPT_LOCK_VALUE);
        final N2oPreFilter localeCodeFilter = createParamEqualFilter(FIELD_LOCALE_CODE);
        final N2oPreFilter dataActionFilter = createParamEqualFilter(FIELD_DATA_ACTION);

        return new N2oPreFilter[] {idFilter, optLockValueFilter, localeCodeFilter, dataActionFilter};
    }

    private N2oPreFilter createParamEqualFilter(String fieldId) {

        final N2oPreFilter preFilter = new N2oPreFilter(fieldId, FilterTypeEnum.EQ);
        preFilter.setParam(fieldId);

        return preFilter;
    }

    private SourceComponent[] createPageFields(DataRecordRequest request) {

        if (request.getStructure().isEmpty()) {
            return new N2oField[0];
        }

        return Stream.concat(
                        createRegularFields(request).stream(),
                        createDynamicFields(request).stream()
                )
                .toArray(SourceComponent[]::new);
    }

    private List<SourceComponent> createRegularFields(DataRecordRequest request) {

        return getSatisfiedResolvers(request.getDataAction())
                .map(resolver -> resolver.createRegularFields(request))
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private List<SourceComponent> createDynamicFields(DataRecordRequest request) {

        final Integer versionId = request.getVersionId();
        final Structure structure = request.getStructure();

        final List<SourceComponent> list = structure.getAttributes().stream()
                .map(attribute -> createDynamicField(versionId, attribute))
                .collect(toList());

        getSatisfiedResolvers(request.getDataAction()).forEach(resolver ->
                resolver.processDynamicFields(request, list)
        );

        return list;
    }

    private N2oStandardField createDynamicField(Integer versionId, Structure.Attribute attribute) {

        final N2oStandardField n2oField;
        if (attribute.isReferenceType()) {
            n2oField = createReferenceField(versionId, attribute);

        } else {
            n2oField = createField(attribute);
        }

        if (attribute.hasIsPrimary()) {
            n2oField.setRequired(Boolean.TRUE.toString());
        }

        return n2oField;
    }

    private N2oStandardField createField(Structure.Attribute attribute) {

        final N2oStandardField n2oField = DataRecordPageUtils.createField(attribute.getType());
        n2oField.setId(addPrefix(attribute.getCode()));
        n2oField.setLabel(attribute.getName());

        return n2oField;
    }

    private N2oStandardField createReferenceField(Integer versionId, Structure.Attribute attribute) {

        final String codeWithPrefix = addPrefix(attribute.getCode());

        final N2oInputSelect referenceField = new N2oInputSelect();
        referenceField.setId(codeWithPrefix);
        referenceField.setLabel(attribute.getName());

        referenceField.setQueryId(REFERENCE_QUERY_ID);
        // NB: value-field-id is deprecated:
        referenceField.setValueFieldId(REFERENCE_VALUE);
        referenceField.setLabelFieldId(REFERENCE_DISPLAY_VALUE);
        referenceField.setDomain(N2oDomain.STRING);

        final N2oPreFilter versionFilter = createValueEqualFilter("versionId", versionId.toString());
        final N2oPreFilter referenceFilter = createValueEqualFilter("reference", attribute.getCode());

        referenceField.setPreFilters(new N2oPreFilter[] {versionFilter, referenceFilter});

        return referenceField;
    }

    @SuppressWarnings("SameParameterValue")
    private N2oPreFilter createValueEqualFilter(String fieldId, String valueAttr) {

        final N2oPreFilter preFilter = new N2oPreFilter(fieldId, FilterTypeEnum.EQ);
        preFilter.setValueAttr(valueAttr);

        return preFilter;
    }

    private Stream<DataRecordPageResolver> getSatisfiedResolvers(String dataAction) {

        if (isEmpty(resolvers))
            return Stream.empty();

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(dataAction));
    }
}

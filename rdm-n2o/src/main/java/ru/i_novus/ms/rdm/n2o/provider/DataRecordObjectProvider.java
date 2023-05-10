package ru.i_novus.ms.rdm.n2o.provider;

import net.n2oapp.framework.api.metadata.SourceMetadata;
import net.n2oapp.framework.api.metadata.global.dao.object.AbstractParameter;
import net.n2oapp.framework.api.metadata.global.dao.object.N2oObject;
import net.n2oapp.framework.api.metadata.global.dao.object.field.ObjectSimpleField;
import net.n2oapp.framework.api.register.DynamicMetadataProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.n2o.api.constant.N2oDomain;
import ru.i_novus.ms.rdm.n2o.api.model.DataRecordRequest;
import ru.i_novus.ms.rdm.n2o.api.resolver.DataRecordObjectResolver;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.springframework.util.CollectionUtils.isEmpty;
import static ru.i_novus.ms.rdm.n2o.api.constant.DataRecordConstants.REFERENCE_VALUE;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addFieldProperty;
import static ru.i_novus.ms.rdm.n2o.api.util.DataRecordUtils.addPrefix;

/**
 * Провайдер для формирования объекта по выполнению операции
 * создания/изменения записи из указанной версии справочника.
 */
@Service
public class DataRecordObjectProvider extends DataRecordBaseProvider implements DynamicMetadataProvider {

    static final String OBJECT_PROVIDER_ID = "dataRecordObject";

    @Autowired
    private Collection<DataRecordObjectResolver> resolvers;

    @Override
    public String getCode() {
        return OBJECT_PROVIDER_ID;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends SourceMetadata> read(String context) {

        DataRecordRequest request = toRequest(context);
        return singletonList(createObject(request));
    }

    @Override
    public Collection<Class<? extends SourceMetadata>> getMetadataClasses() {
        return singletonList(N2oObject.class);
    }

    private N2oObject createObject(DataRecordRequest request) {

        N2oObject object = new N2oObject();
        object.setOperations(createOperations(request));

        return object;
    }

    private N2oObject.Operation[] createOperations(DataRecordRequest request) {

        List<N2oObject.Operation> operations = getSatisfiedResolvers(request.getDataAction())
                .map(resolver -> createOperation(resolver, request))
                .collect(toList());

        return operations.toArray(N2oObject.Operation[]::new);
    }

    private N2oObject.Operation createOperation(DataRecordObjectResolver resolver, DataRecordRequest request) {

        N2oObject.Operation operation = resolver.createOperation(request);
        operation.setInFields(Stream.concat(
                resolver.createRegularParams(request).stream(),
                createDynamicParams(request).stream())
                .toArray(AbstractParameter[]::new));

        return operation;
    }

    private List<AbstractParameter> createDynamicParams(DataRecordRequest request) {

        Structure structure = request.getStructure();
        if (structure.isEmpty())
            return emptyList();

        return structure.getAttributes().stream().map(this::createParam).collect(toList());
    }

    private AbstractParameter createParam(Structure.Attribute attribute) {

        final String mappingArgumentFormat = "['row'].data['%s']";

        final String codeWithPrefix = addPrefix(attribute.getCode());

        ObjectSimpleField parameter = switch (attribute.getType()) {

            case STRING, INTEGER, FLOAT, DATE, BOOLEAN ->
                createParam(codeWithPrefix, attribute.getType());

            case REFERENCE ->
                createReferenceParam(codeWithPrefix);

            default -> throw new IllegalArgumentException("attribute type not supported");
        };

        parameter.setMapping(String.format(mappingArgumentFormat, attribute.getCode()));

        return parameter;
    }

    /** Заполнение полей примитивного параметра. */
    @SuppressWarnings("java:S1199")
    private ObjectSimpleField createParam(String codeWithPrefix, FieldType type) {

        ObjectSimpleField parameter = new ObjectSimpleField();

        parameter.setId(codeWithPrefix);
        parameter.setDomain(N2oDomain.fieldTypeToDomain(type));

        switch (type) {

            case DATE ->
                parameter.setNormalize("T(ru.i_novus.ms.rdm.api.util.TimeUtils).parseLocalDate(#this)");

            case BOOLEAN ->
                parameter.setDefaultValue("false");

            default -> {
                // Nothing to do.
            }
        }

        return parameter;
    }

    /** Заполнение полей параметра-ссылки. */
    private ObjectSimpleField createReferenceParam(String codeWithPrefix) {

        ObjectSimpleField parameter = new ObjectSimpleField();

        parameter.setId(addFieldProperty(codeWithPrefix, REFERENCE_VALUE));
        parameter.setDomain(N2oDomain.STRING);

        return parameter;
    }

    private Stream<DataRecordObjectResolver> getSatisfiedResolvers(String dataAction) {

        if (isEmpty(resolvers))
            return Stream.empty();

        return resolvers.stream()
                .filter(resolver -> resolver.isSatisfied(dataAction));
    }
}

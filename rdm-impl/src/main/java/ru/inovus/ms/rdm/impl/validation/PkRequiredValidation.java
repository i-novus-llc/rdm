package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.Structure;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Проверка на обязательность по первичным ключам.
 */
public class PkRequiredValidation extends ErrorAttributeHolderValidation {

    public static final String VALIDATION_REQUIRED_PK_ERR_EXCEPTION_CODE = "validation.required.pk.err";

    private Map<String, Object> row;

    private Structure structure;

    public PkRequiredValidation(Row row, Structure structure) {
        this.row = row.getData();
        this.structure = structure;
    }

    public PkRequiredValidation(Row row, Structure structure, Set<String> excludeAttributes) {
        this(row, structure);
        setErrorAttributes(excludeAttributes);
    }

    @Override
    public List<Message> validate() {
        List<Structure.Attribute> primaries = structure.getPrimary();
        return structure.getAttributes().stream()
                .filter(attribute -> !isErrorAttribute(attribute.getCode()))
                .filter(primaries::contains)
                .filter(this::isPrimaryEmpty)
                .peek(this::addErrorAttribute)
                .map(attribute -> new Message(VALIDATION_REQUIRED_PK_ERR_EXCEPTION_CODE, attribute.getName()))
                .collect(Collectors.toList());
    }

    private boolean isPrimaryEmpty(Structure.Attribute primaryKey){
        Object value = row.get(primaryKey.getCode());
        return value == null || "".equals(String.valueOf(value).trim());
    }

    private void addErrorAttribute(Structure.Attribute attribute) {
        addErrorAttribute(attribute.getCode());
    }
}

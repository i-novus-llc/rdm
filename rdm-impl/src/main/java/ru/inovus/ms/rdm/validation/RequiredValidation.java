package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by znurgaliev on 14.08.2018.
 */
public class RequiredValidation extends ErrorAttributeHolderValidator {

    public static final String ERROR_CODE = "validation.required.err";

    private Map<String, Object> row;

    private Structure structure;

    public RequiredValidation(Row row, Structure structure) {
        this.row = row.getData();
        this.structure = structure;
    }

    public RequiredValidation(Row row, Structure structure, Set<String> excludeAttributes) {
        this(row, structure);
        setErrorAttributes(excludeAttributes);
    }

    @Override
    public List<Message> validate() {
        return structure.getAttributes().stream()
                .filter(attribute -> getErrorAttributes() == null || !getErrorAttributes().contains(attribute.getCode()))
                .filter(this::isValueInvalid)
                .peek(this::addErrorAttribute)
                .map(attribute -> new Message(ERROR_CODE, attribute.getCode()))
                .collect(Collectors.toList());
    }

    private boolean isValueInvalid(Structure.Attribute attribute){
        Object value = row.get(attribute.getCode());
        return attribute.getIsRequired() && (value == null || "".equals(value));
    }

    private void addErrorAttribute(Structure.Attribute attribute) {
        addErrorAttribute(attribute.getCode());
    }
}

package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;
import ru.inovus.ms.rdm.n2o.model.Structure;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by znurgaliev on 14.08.2018.
 */
public class PkRequiredValidation extends ErrorAttributeHolderValidation {

    static final String REQUIRED_ERROR_CODE = "validation.required.err";

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
                .filter(attribute -> getErrorAttributes() == null || !getErrorAttributes().contains(attribute.getCode()))
                .filter(primaries::contains)
                .filter(this::isPKBlank)
                .peek(this::addErrorAttribute)
                .map(attribute -> new Message(REQUIRED_ERROR_CODE, attribute.getName()))
                .collect(Collectors.toList());
    }

    private boolean isPKBlank(Structure.Attribute pK){
        Object value = row.get(pK.getCode());
        return value == null || "".equals(String.valueOf(value).trim());
    }

    private void addErrorAttribute(Structure.Attribute attribute) {
        addErrorAttribute(attribute.getCode());
    }
}

package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.Row;

import java.util.*;

/**
 * Created by znurgaliev on 20.11.2018.
 */
public abstract class AppendRowValidation extends ErrorAttributeHolderValidation {

    private final Map<Map<String, Object>, Long> buffer = new LinkedHashMap<>();

    public void appendRow(Row row) {
        if (row != null) {
            Map<String, Object> rowMap = new HashMap<>(row.getData());
            rowMap.entrySet().removeIf(entry -> getErrorAttributes().contains(entry.getKey()));
            buffer.put(rowMap, row.getSystemId());
        }
    }

    @Override
    public List<Message> validate() {
        if (buffer.isEmpty())
            throw new RdmException("Missing row to validate, append row before validation");
        List<Message> messages = new ArrayList<>();
        buffer.keySet().stream()
                .peek(map -> map.entrySet().removeIf(entry -> getErrorAttributes().contains(entry.getKey())))
                .map(attributeValues -> validate(buffer.get(attributeValues), attributeValues))
                .forEach(messages::addAll);
        buffer.clear();
        return messages;
    }

    protected abstract List<Message> validate(Long systemId, Map<String, Object> attributeValues);
}

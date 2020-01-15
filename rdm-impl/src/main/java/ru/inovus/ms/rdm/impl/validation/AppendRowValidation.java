package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.api.exception.RdmException;
import ru.inovus.ms.rdm.api.model.refdata.Row;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

/**
 * Проверка одновременно нескольких строк.
 */
public abstract class AppendRowValidation extends ErrorAttributeHolderValidation {

    private final Map<Map<String, Object>, Long> buffer = new LinkedHashMap<>();

    public void appendRow(Row row) {
        if (row != null) {
            Map<String, Object> rowData = new HashMap<>(row.getData());
            rowData.entrySet().removeIf(entry -> getErrorAttributes().contains(entry.getKey()));
            buffer.put(rowData, row.getSystemId());
        }
    }

    @Override
    public List<Message> validate() {
        if (buffer.isEmpty())
            throw new RdmException("Missing refData to validate, append refData before validation");

        buffer.keySet().forEach(map ->
                map.entrySet().removeIf(entry -> getErrorAttributes().contains(entry.getKey()))
        );

        List<Message> messages = buffer.entrySet().stream().flatMap(this::validateEntry).collect(toList());
        buffer.clear();

        return messages;
    }

    private Stream<Message> validateEntry(Map.Entry<Map<String, Object>, Long> entry) {
        return validate(entry.getValue(), entry.getKey()).stream();
    }

    protected abstract List<Message> validate(Long systemId, Map<String, Object> rowData);
}

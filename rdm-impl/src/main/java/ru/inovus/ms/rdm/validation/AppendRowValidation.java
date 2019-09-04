package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.exception.RdmException;
import ru.inovus.ms.rdm.model.refdata.Row;

import java.util.*;

import static java.util.stream.Collectors.toList;

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
            throw new RdmException("Missing refData to validate, append refData before validation");

        buffer.keySet().forEach(map ->
                map.entrySet().removeIf(entry -> getErrorAttributes().contains(entry.getKey()))
        );

        List<Message> messages = buffer.entrySet().stream()
                .flatMap(entry -> validate(entry.getValue(), entry.getKey()).stream())
                .collect(toList());
        buffer.clear();

        return messages;
    }

    protected abstract List<Message> validate(Long systemId, Map<String, Object> attributeValues);
}

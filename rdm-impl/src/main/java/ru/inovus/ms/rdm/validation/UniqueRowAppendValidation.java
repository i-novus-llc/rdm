package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by znurgaliev on 15.08.2018.
 */
public class UniqueRowAppendValidation extends ErrorAttributeHolderValidation {

    public static final String ERROR_CODE = "validation.not.unique.pk.err";

    Set<String> uniqueRowAttributes;

    Set<Map<String, Object>> uniqueRow;

    LinkedList<Map<String, Object>> buffer;

    Structure structure;

    public UniqueRowAppendValidation(Structure structure) {
        this.structure = structure;
        this.uniqueRowAttributes = structure.getAttributes().stream()
                .filter(Structure.Attribute::getIsPrimary)
                .map(Structure.Attribute::getCode)
                .collect(Collectors.toSet());
        this.uniqueRow = new HashSet<>();
        this.buffer = new LinkedList<>();
    }

    public void appendRow(Row row) {
        if (row != null) {
            Map<String, Object> rowMap = new HashMap<>(row.getData());
            rowMap.entrySet().removeIf(entry -> !uniqueRowAttributes.contains(entry.getKey()));
            rowMap.entrySet().removeIf(entry -> getErrorAttributes().contains(entry.getKey()));
            buffer.add(rowMap);
        }
    }

    @Override
    public List<Message> validate() {
        if (uniqueRowAttributes.isEmpty()) return Collections.emptyList();
        List<Message> messages = buffer.stream()
                .filter(row -> row.keySet().containsAll(uniqueRowAttributes))
                .filter(this::isNotUnique)
                .map(map -> new Message(ERROR_CODE,
                        map.entrySet().stream()
                                .map(e -> structure.getAttribute(e.getKey()).getName() + "\" - \"" + e.getValue())
                                .collect(Collectors.joining("\", \""))))
                .collect(Collectors.toList());
        buffer.clear();
        return messages;
    }

    private boolean isNotUnique(Map<String, Object> row) {
        return !uniqueRow.add(row);
    }
}

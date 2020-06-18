package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.api.model.Structure;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toSet;

/**
 * Проверка на уникальность добавляемых строк по первичным ключам между собой.
 */
public class PkUniqueRowAppendValidation extends AppendRowValidation {

    private static final String VALIDATION_NOT_UNIQUE_PK_ERR_EXCEPTION_CODE = "validation.not.unique.pk.err";

    private final Set<String> primaryKeyCodes;

    private final Set<Map<String, Object>> uniqueRowSet;

    private final Structure structure;

    public PkUniqueRowAppendValidation(Structure structure) {
        this.structure = structure;
        this.primaryKeyCodes = structure.getPrimary().stream().map(Structure.Attribute::getCode).collect(toSet());
        this.uniqueRowSet = new HashSet<>();
    }

    @Override
    protected List<Message> validate(Long systemId, Map<String, Object> rowData) {

        if (primaryKeyCodes.isEmpty() || !rowData.keySet().containsAll(primaryKeyCodes))
            return Collections.emptyList();

        rowData.entrySet().removeIf(entry -> !primaryKeyCodes.contains(entry.getKey()));
        if (!uniqueRowSet.add(rowData)) {
            return singletonList(new Message(VALIDATION_NOT_UNIQUE_PK_ERR_EXCEPTION_CODE,
                    rowData.entrySet().stream()
                            .map(e -> structure.getAttribute(e.getKey()).getName() + "\" - \"" + e.getValue())
                            .collect(Collectors.joining("\", \""))));
        }

        return emptyList();
    }
}

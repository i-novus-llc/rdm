package ru.inovus.ms.rdm.impl.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.api.model.Structure;
import ru.inovus.ms.rdm.api.util.RowUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Проверка на уникальность добавляемых строк по первичным ключам между собой.
 */
public class PkUniqueRowAppendValidation extends AppendRowValidation {

    private static final String NOT_UNIQUE_PK_ERR = "validation.not.unique.pk.err";

    private final Set<String> primaryKeyCodes;

    private final Set<Map<String, Object>> uniqueRowSet;

    private final Structure structure;

    public PkUniqueRowAppendValidation(Structure structure) {
        this.structure = structure;
        this.primaryKeyCodes = structure.getPrimary().stream()
                .map(Structure.Attribute::getCode)
                .collect(Collectors.toSet());
        this.uniqueRowSet = new HashSet<>();
    }

    @Override
    protected List<Message> validate(Long systemId, Map<String, Object> rowData) {

        if (primaryKeyCodes.isEmpty() || !rowData.keySet().containsAll(primaryKeyCodes))
            return emptyList();

        rowData.entrySet().removeIf(entry -> !primaryKeyCodes.contains(entry.getKey()));
        if (!uniqueRowSet.add(rowData)) {
            return singletonList(new Message(NOT_UNIQUE_PK_ERR,
                    RowUtils.toNamedValues(rowData, structure.getPrimary())
            ));
        }

        return emptyList();
    }
}

package ru.inovus.ms.rdm.validation;

import net.n2oapp.platform.i18n.Message;
import ru.inovus.ms.rdm.model.Structure;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

/**
 * Created by znurgaliev on 15.08.2018.
 */
public class PkUniqueRowAppendValidation extends AppendRowValidation {

    public static final String NOT_UNIQUE_PK_ERR = "validation.not.unique.pk.err";

    private final Set<String> uniqueRowAttributes;

    private final Set<Map<String, Object>> uniqueRow;

    private final Structure structure;

    public PkUniqueRowAppendValidation(Structure structure) {
        this.structure = structure;
        this.uniqueRowAttributes = structure.getAttributes().stream()
                .filter(Structure.Attribute::getIsPrimary)
                .map(Structure.Attribute::getCode)
                .collect(Collectors.toSet());
        this.uniqueRow = new HashSet<>();
    }

    @Override
    protected List<Message> validate(Long systemId, Map<String, Object> attributeValues) {
        if (uniqueRowAttributes.isEmpty() || !attributeValues.keySet().containsAll(uniqueRowAttributes))
            return Collections.emptyList();
        attributeValues.entrySet().removeIf(entry -> !uniqueRowAttributes.contains(entry.getKey()));
        if (!uniqueRow.add(attributeValues)) {
            return singletonList(new Message(NOT_UNIQUE_PK_ERR,
                    attributeValues.entrySet().stream()
                            .map(e -> structure.getAttribute(e.getKey()).getName() + "\" - \"" + e.getValue())
                            .collect(Collectors.joining("\", \""))));
        }

        return emptyList();
    }
}

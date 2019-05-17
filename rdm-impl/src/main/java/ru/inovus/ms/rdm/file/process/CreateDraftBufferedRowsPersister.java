package ru.inovus.ms.rdm.file.process;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.util.ConverterUtil.field;
import static ru.inovus.ms.rdm.util.ConverterUtil.fields;

public class CreateDraftBufferedRowsPersister implements RowsProcessor {

    private BufferedRowsPersister bufferedRowsPersister;

    private DraftDataService draftDataService;

    private boolean isFirstRowAppended;

    private int size = 100;

    private BiConsumer saveDraftConsumer;

    private String storageCode;
    private Structure structure = null;
    private Set<String> allKeys = new LinkedHashSet<>();

    public CreateDraftBufferedRowsPersister(DraftDataService draftDataService, BiConsumer<String, Structure> saveDraftConsumer) {
        this.draftDataService = draftDataService;
        this.saveDraftConsumer = saveDraftConsumer;
    }

    public CreateDraftBufferedRowsPersister(DraftDataService draftDataService, int size, BiConsumer<String, Structure> saveDraftConsumer) {
        this.draftDataService = draftDataService;
        this.size = size;
        this.saveDraftConsumer = saveDraftConsumer;
    }

    @Override
    public Result append(Row row) {
        if (!isFirstRowAppended) {
            allKeys.addAll(row.getData().keySet());
            structure = stringStructure(allKeys);
            storageCode = draftDataService.createDraft(fields(structure));
            this.bufferedRowsPersister = new BufferedRowsPersister(size, draftDataService, storageCode, structure);
            isFirstRowAppended = true;
        } else {
            updateStructure(row);
        }
        return bufferedRowsPersister.append(row);
    }

    private void updateStructure(Row row) {
        List<String> newKeys = row.getData().keySet().stream()
                .filter(k -> !allKeys.contains(k))
                .collect(Collectors.toList());
        if (allKeys.addAll(newKeys)) {
            newKeys.stream()
                    .map(this::stringAttribute)
                    .peek(attribute -> draftDataService.addField(storageCode, field(attribute)))
                    .forEach(attribute -> structure.getAttributes().add(attribute));
            bufferedRowsPersister.setStructure(structure);
        }
    }

    private Structure stringStructure(Set<String> keySet) {
        List<Structure.Attribute> attributes = new ArrayList<>();
        keySet.forEach(columnName ->
                attributes.add(stringAttribute(columnName))
        );
        return new Structure(attributes, null);
    }

    private Structure.Attribute stringAttribute(String attrCode) {
        return Structure.Attribute.build(attrCode, attrCode, FieldType.STRING, attrCode);
    }

    @Override
    public Result process() {
        Result result = bufferedRowsPersister != null ? bufferedRowsPersister.process() : null;
        saveDraftConsumer.accept(storageCode, structure);
        return result;
    }
}

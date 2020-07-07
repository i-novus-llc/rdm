package ru.inovus.ms.rdm.impl.file.process;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.api.model.Result;
import ru.inovus.ms.rdm.api.model.refdata.Row;
import ru.inovus.ms.rdm.api.model.Structure;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.impl.util.ConverterUtil.field;
import static ru.inovus.ms.rdm.impl.util.ConverterUtil.fields;

public class CreateDraftBufferedRowsPersister implements RowsProcessor {

    private BufferedRowsPersister bufferedRowsPersister;

    private DraftDataService draftDataService;

    private boolean isFirstRowAppended;

    private int bufferSize = 100;

    private BiConsumer<String, Structure> saveDraftConsumer;

    private String storageCode;
    private Structure structure = null;
    private Set<String> allKeys = new LinkedHashSet<>();

    public CreateDraftBufferedRowsPersister(DraftDataService draftDataService,
                                            BiConsumer<String, Structure> saveDraftConsumer) {
        this.draftDataService = draftDataService;
        this.saveDraftConsumer = saveDraftConsumer;
    }

    public CreateDraftBufferedRowsPersister(DraftDataService draftDataService, int bufferSize,
                                            BiConsumer<String, Structure> saveDraftConsumer) {
        this.draftDataService = draftDataService;
        this.bufferSize = bufferSize;
        this.saveDraftConsumer = saveDraftConsumer;
    }

    @Override
    public Result append(Row row) {
        if (!isFirstRowAppended) {
            allKeys.addAll(row.getData().keySet());
            structure = stringStructure(allKeys);
            storageCode = draftDataService.createDraft(fields(structure));
            this.bufferedRowsPersister = new BufferedRowsPersister(bufferSize, draftDataService, storageCode, structure);
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
            newKeys.forEach(this::addAttribute);
            bufferedRowsPersister.setStructure(structure);
        }
    }

    private Structure stringStructure(Set<String> keySet) {

        List<Structure.Attribute> attributes = new ArrayList<>();
        keySet.forEach(columnName -> attributes.add(stringAttribute(columnName)));

        return new Structure(attributes, null);
    }

    private Structure.Attribute stringAttribute(String attrCode) {
        return Structure.Attribute.build(attrCode, attrCode, FieldType.STRING, attrCode);
    }

    private void addAttribute(String columnName) {

        Structure.Attribute attribute = stringAttribute(columnName);
        draftDataService.addField(storageCode, field(attribute));
        structure.getAttributes().add(attribute);
    }

    @Override
    public Result process() {

        Result result = bufferedRowsPersister != null ? bufferedRowsPersister.process() : null;
        saveDraftConsumer.accept(storageCode, structure);

        return result;
    }
}

package ru.i_novus.ms.rdm.impl.file.process;

import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.Structure;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import static ru.i_novus.ms.rdm.impl.util.ConverterUtil.field;

public class CreateDraftBufferedRowsPersister implements RowsProcessor {

    private final DraftDataService draftDataService;

    private final Function<Structure, String> createDraftStorage;

    private final BiConsumer<String, Structure> saveDraftConsumer;

    private final Set<String> allKeys = new LinkedHashSet<>();

    private int bufferSize = 100;

    private boolean isFirstRowAppended;

    private Structure structure = null;

    private String draftCode;

    private BufferedRowsPersister bufferedRowsPersister;

    public CreateDraftBufferedRowsPersister(DraftDataService draftDataService,
                                            Function<Structure, String> createDraftStorage,
                                            BiConsumer<String, Structure> saveDraftConsumer) {
        this.draftDataService = draftDataService;
        this.createDraftStorage = createDraftStorage;
        this.saveDraftConsumer = saveDraftConsumer;
    }

    public void setBufferSize(int bufferSize) {
        this.bufferSize = bufferSize;
    }

    @Override
    public Result append(Row row) {

        if (!isFirstRowAppended) {
            allKeys.addAll(row.getData().keySet());
            structure = stringStructure(allKeys);
            draftCode = createDraftStorage.apply(structure);

            this.bufferedRowsPersister = new BufferedRowsPersister(draftDataService, draftCode, structure, bufferSize);

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
        draftDataService.addField(draftCode, field(attribute));
        structure.getAttributes().add(attribute);
    }

    @Override
    public Result process() {

        Result result = bufferedRowsPersister != null ? bufferedRowsPersister.process() : null;
        saveDraftConsumer.accept(draftCode, structure);

        return result;
    }
}

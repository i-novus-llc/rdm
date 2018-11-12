package ru.inovus.ms.rdm.file;

import ru.i_novus.platform.datastorage.temporal.enums.FieldType;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Row;
import ru.inovus.ms.rdm.model.Structure;
import ru.inovus.ms.rdm.util.ConverterUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

public class CreateDraftBufferedRowsPersister implements RowsProcessor {

    private BufferedRowsPersister bufferedRowsPersister;

    private DraftDataService draftDataService;

    private boolean isFirstRowAppended;

    private int size = 100;

    private BiConsumer consumer;

    public CreateDraftBufferedRowsPersister(DraftDataService draftDataService, BiConsumer<String, Structure> consumer) {
        this.draftDataService = draftDataService;
        this.consumer = consumer;
    }

    public CreateDraftBufferedRowsPersister(DraftDataService draftDataService, int size, BiConsumer<String, Structure> consumer) {
        this.draftDataService = draftDataService;
        this.size = size;
        this.consumer = consumer;
    }

    @Override
    public Result append(Row row) {
        if (!isFirstRowAppended) {
            Structure structure = fields(row);
            String storageCode = draftDataService.createDraft(ConverterUtil.fields(structure));
            consumer.accept(storageCode, structure);
            this.bufferedRowsPersister = new BufferedRowsPersister(size, draftDataService, storageCode, structure);
            isFirstRowAppended = true;
        }
        return bufferedRowsPersister.append(row);
    }

    private Structure fields(Row row) {
        List<Structure.Attribute> attributes = new ArrayList<>();
        row.getData().keySet().forEach(columnName ->
            attributes.add(Structure.Attribute.build(columnName, columnName, FieldType.STRING, false, columnName))
        );
        return new Structure(attributes, null);
    }

    @Override
    public Result process() {
        return bufferedRowsPersister != null ? bufferedRowsPersister.process() : null;
    }
}

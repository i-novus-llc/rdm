package ru.inovus.ms.rdm.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.i_novus.platform.datastorage.temporal.service.FieldFactory;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static ru.inovus.ms.rdm.util.ConverterUtil.rowValue;

public class BufferedRowsPersister implements RowsProcessor {

    private static final Logger logger = LoggerFactory.getLogger(BufferedRowsPersister.class);

    private int size = 100;

    private List<Row> buffer = new ArrayList<>();

    private DraftDataService draftDataService;

    private String storageCode;

    private Structure structure;

    private Result result = new Result(0, 0, null);

    private FieldFactory fieldFactory;

    public BufferedRowsPersister(DraftDataService draftDataService, FieldFactory fieldFactory, String storageCode, Structure structure) {
        this.draftDataService = draftDataService;
        this.fieldFactory = fieldFactory;
        this.storageCode = storageCode;
        this.structure = structure;
    }

    public BufferedRowsPersister(int size, DraftDataService draftDataService, FieldFactory fieldFactory, String storageCode, Structure structure) {
        this.size = size;
        this.draftDataService = draftDataService;
        this.fieldFactory = fieldFactory;
        this.storageCode = storageCode;
        this.structure = structure;
    }

    @Override
    public Result append(Row row) {
        buffer.add(row);

        if (buffer.size() == size) {
            save();
            buffer.clear();
        }
        return this.result;
    }

    @Override
    public Result process() {
        save();
        return result;
    }

    private void save() {
        if (buffer.isEmpty()) {
            return;
        }
        List<RowValue> rowValues = buffer.stream().map(row -> rowValue(row, structure, fieldFactory)).collect(Collectors.toList());
        try {
            draftDataService.addRows(storageCode, rowValues);
            this.result = this.result.addResult(new Result(buffer.size(), buffer.size(), null));
        } catch (Exception e) {
            this.result = this.result.addResult(new Result(0, buffer.size(), Collections.singletonList(e.getMessage())));
            logger.error("can not add rows", e);
        }
    }


}

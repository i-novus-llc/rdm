package ru.inovus.ms.rdm.file;

import ru.i_novus.platform.datastorage.temporal.model.value.RowValue;
import ru.i_novus.platform.datastorage.temporal.service.DraftDataService;
import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.Structure;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class BufferedRowsPersister implements RowsProcessor {

    private int size = 100;

    private List<Row> buffer = new ArrayList<>();

    private DraftDataService draftDataService;

    private String storageCode;

    private Structure structure;

    private Result result = new Result(0, 0, null);

    public BufferedRowsPersister(DraftDataService draftDataService, String storageCode, Structure structure) {
        this.draftDataService = draftDataService;
        this.storageCode = storageCode;
        this.structure = structure;
    }

    public BufferedRowsPersister(int size, DraftDataService draftDataService, String storageCode, Structure structure) {
        this.size = size;
        this.draftDataService = draftDataService;
        this.storageCode = storageCode;
        this.structure = structure;
    }

    @Override
    public Result append(Row row) {
        buffer.add(row);

        if (buffer.size() == size) {
            //todo
            try {
                save();
                this.result.addResult(new Result(buffer.size(), buffer.size(), null));
            } catch (Exception e) {
                this.result.addResult(new Result(0, buffer.size(), Collections.singletonList(e.getMessage())));
            }
            buffer.clear();
        }
        return this.result;
    }

    @Override
    public Result process() {
        Result result = this.result.addResult(new Result(buffer.size(), buffer.size(), null));
        save();
        return result;
    }

    private void save() {
        if (buffer.isEmpty()) {
            return;
        }

        List<RowValue> rowValues = buffer.stream().map(this::rowValue).collect(Collectors.toList());
        draftDataService.addRows(storageCode, rowValues);
    }

    private RowValue rowValue(Row row) {
//      todo
        return null;
    }
}

package ru.inovus.ms.mdm.file;

import ru.inovus.ms.mdm.model.FileData;
import ru.inovus.ms.mdm.model.Result;

import java.util.Iterator;

public abstract class FilePerRowValidator implements FileProcessor, Iterator<Row> {

    private RowMapper rowMapper;
    private RowsProcessor rowsProcessor;

    public FilePerRowValidator(RowMapper rowMapper, RowsProcessor rowsProcessor) {
        this.rowMapper = rowMapper;
        this.rowsProcessor = rowsProcessor;
    }

    public Result process(FileData file) {
        while (hasNext()) {
            rowsProcessor.append(rowMapper.map(next()));
        }
        return rowsProcessor.process();
    }

}

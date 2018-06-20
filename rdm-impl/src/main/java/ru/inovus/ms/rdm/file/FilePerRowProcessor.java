package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.FileData;
import ru.inovus.ms.rdm.model.Result;

import java.util.Iterator;

public abstract class FilePerRowProcessor implements FileProcessor, Iterator<Row> {

    private RowMapper rowMapper;
    private RowsProcessor rowsProcessor;

    public FilePerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {
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

package ru.i_novus.ms.rdm.impl.file.process;

import net.n2oapp.platform.i18n.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.refdata.Row;
import ru.i_novus.ms.rdm.api.util.row.RowMapper;
import ru.i_novus.ms.rdm.api.util.row.RowsProcessor;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.function.Supplier;

import static java.util.Collections.singletonList;

public abstract class FilePerRowProcessor implements FileProcessor<Result>, Iterator<Row>, Closeable {

    private static final Logger logger = LoggerFactory.getLogger(FilePerRowProcessor.class);

    private final RowMapper rowMapper;
    private final RowsProcessor rowsProcessor;

    public FilePerRowProcessor(RowMapper rowMapper, RowsProcessor rowsProcessor) {

        this.rowMapper = rowMapper;
        this.rowsProcessor = rowsProcessor;
    }

    protected abstract void setFile(InputStream inputStream);

    public Result process(Supplier<InputStream> fileSource) {

        try (InputStream inputStream = fileSource.get()) {
            setFile(inputStream);

            while (hasNext()) {
                rowsProcessor.append(rowMapper.map(next()));
            }

        } catch (IOException e) {
            logger.error("cannot get InputStream", e);
            return new Result(0, 0, singletonList(new Message("io.error", e.getMessage())));
        }

        return rowsProcessor.process();
    }

}

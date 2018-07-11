package ru.inovus.ms.rdm.file.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.file.Row;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;

public abstract class PerRowFileGenerator implements FileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PerRowFileGenerator.class);

    protected abstract OutputStream getOutputStream();

    protected abstract void write(Row row, OutputStream outputStream);

    @Override
    public void generate(Iterator<Row> rowsIterator) {
        try(OutputStream outputStream = getOutputStream()) {
            if (rowsIterator.hasNext()) {
                write(rowsIterator.next(), outputStream);
            }
        } catch (IOException e) {
            logger.error("cannot get output stream", e);
        }
    }
}

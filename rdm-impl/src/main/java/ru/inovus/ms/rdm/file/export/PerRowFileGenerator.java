package ru.inovus.ms.rdm.file.export;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.inovus.ms.rdm.file.Row;

import java.io.OutputStream;
import java.util.Iterator;

public abstract class PerRowFileGenerator implements FileGenerator {

    private static final Logger logger = LoggerFactory.getLogger(PerRowFileGenerator.class);

    private OutputStream outputStream;

    Iterator<Row> rowIterator;

    public PerRowFileGenerator(Iterator<Row> rowIterator) {
        this.rowIterator = rowIterator;
    }

    protected abstract void startWrite();

    protected abstract void write(Row row);

    protected abstract void endWrite();

    protected OutputStream getOutputStream() {
        return outputStream;
    }

    @Override
    public void generate(OutputStream os) {
        outputStream = os;
        startWrite();
        while (rowIterator.hasNext()) {
            write(rowIterator.next());
        }
        endWrite();
        outputStream = null;
    }
}

package ru.inovus.ms.rdm.file.export;

import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.Structure;

import java.io.OutputStream;
import java.util.Iterator;

public abstract class PerRowFileGenerator implements FileGenerator {

    private OutputStream outputStream;

    private Structure structure;

    private Iterator<Row> rowIterator;

    public PerRowFileGenerator(Iterator<Row> rowIterator) {
        this.rowIterator = rowIterator;
    }

    public PerRowFileGenerator(Iterator<Row> rowIterator, Structure structure) {
        this.rowIterator = rowIterator;
        this.structure = structure;
    }

    protected abstract void startWrite();

    protected abstract void write(Row row);

    protected abstract void endWrite();

    protected OutputStream getOutputStream() {
        return outputStream;
    }

    protected Structure getStructure(){
        return structure;
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

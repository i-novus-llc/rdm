package ru.inovus.ms.rdm.file.export;

import ru.inovus.ms.rdm.file.Row;

import java.util.Iterator;

public interface FileGenerator {

    void generate(Iterator<Row> rowsIterator);

}

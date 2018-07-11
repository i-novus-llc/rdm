package ru.inovus.ms.rdm.file.export;

import ru.inovus.ms.rdm.file.Row;
import ru.inovus.ms.rdm.model.RefBookVersion;
import ru.inovus.ms.rdm.service.VersionService;

import java.util.Iterator;

public class VersionDataIterator implements Iterator<Row> {

    private RefBookVersion refBookVersion;

    private VersionService versionService;

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Row next() {
        return null;
    }
}

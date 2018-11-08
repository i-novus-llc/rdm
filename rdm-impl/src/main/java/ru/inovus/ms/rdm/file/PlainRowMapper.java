package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.Row;

public class PlainRowMapper implements RowMapper {
    @Override
    public Row map(Row inputRow) {
        return inputRow;
    }
}

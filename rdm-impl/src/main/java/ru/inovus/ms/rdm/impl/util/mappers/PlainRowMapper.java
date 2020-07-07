package ru.inovus.ms.rdm.impl.util.mappers;

import ru.inovus.ms.rdm.api.model.refdata.Row;

public class PlainRowMapper implements RowMapper {
    @Override
    public Row map(Row inputRow) {
        return inputRow;
    }
}

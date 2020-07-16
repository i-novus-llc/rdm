package ru.i_novus.ms.rdm.impl.util.mappers;

import ru.i_novus.ms.rdm.api.model.refdata.Row;

public class PlainRowMapper implements RowMapper {
    @Override
    public Row map(Row inputRow) {
        return inputRow;
    }
}

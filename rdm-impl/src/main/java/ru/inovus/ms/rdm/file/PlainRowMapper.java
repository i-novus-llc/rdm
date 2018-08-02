package ru.inovus.ms.rdm.file;

public class PlainRowMapper implements RowMapper {
    @Override
    public Row map(Row inputRow) {
        return inputRow;
    }
}

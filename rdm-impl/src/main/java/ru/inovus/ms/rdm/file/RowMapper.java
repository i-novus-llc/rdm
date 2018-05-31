package ru.inovus.ms.rdm.file;

public interface RowMapper {

    default Row map(Row inputRow) {
        return inputRow;
    }
}

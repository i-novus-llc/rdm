package ru.inovus.ms.mdm.file;

public interface RowMapper {

    default Row map(Row inputRow) {
        return inputRow;
    }
}

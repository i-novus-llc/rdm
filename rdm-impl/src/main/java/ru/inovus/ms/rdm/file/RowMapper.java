package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.refdata.Row;

public interface RowMapper {

     Row map(Row inputRow);
}

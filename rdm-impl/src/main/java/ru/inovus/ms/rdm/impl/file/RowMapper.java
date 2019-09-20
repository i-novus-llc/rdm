package ru.inovus.ms.rdm.impl.file;

import ru.inovus.ms.rdm.api.model.refdata.Row;

public interface RowMapper {

     Row map(Row inputRow);
}

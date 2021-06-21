package ru.i_novus.ms.rdm.api.util.row;

import ru.i_novus.ms.rdm.api.model.refdata.Row;

/**
 * Отображатель входящих данных в данные версии справочника.
 */
public interface RowMapper {

     Row map(Row inputRow);
}

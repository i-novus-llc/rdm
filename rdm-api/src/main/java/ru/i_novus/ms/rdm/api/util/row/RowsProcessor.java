package ru.i_novus.ms.rdm.api.util.row;

import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.refdata.Row;

/**
 * Обработчик входящих данных для сохранения в версии справочника.
 *
 */
public interface RowsProcessor {

    Result append(Row row);

    Result process();
}

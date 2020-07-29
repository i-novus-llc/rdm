package ru.i_novus.ms.rdm.impl.file.process;

import ru.i_novus.ms.rdm.api.model.Result;
import ru.i_novus.ms.rdm.api.model.refdata.Row;

public interface RowsProcessor {

    Result append(Row row);

    Result process();

}

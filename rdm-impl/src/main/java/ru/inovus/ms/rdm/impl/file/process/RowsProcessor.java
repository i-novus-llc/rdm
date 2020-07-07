package ru.inovus.ms.rdm.impl.file.process;

import ru.inovus.ms.rdm.api.model.Result;
import ru.inovus.ms.rdm.api.model.refdata.Row;

public interface RowsProcessor {

    Result append(Row row);

    Result process();

}

package ru.inovus.ms.rdm.file.process;

import ru.inovus.ms.rdm.model.Result;
import ru.inovus.ms.rdm.model.refdata.Row;

public interface RowsProcessor {

    Result append(Row row);

    Result process();

}

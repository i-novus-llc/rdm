package ru.inovus.ms.rdm.file.process;

import ru.inovus.ms.rdm.n2o.model.Result;
import ru.inovus.ms.rdm.n2o.model.refdata.Row;

public interface RowsProcessor {

    Result append(Row row);

    Result process();

}

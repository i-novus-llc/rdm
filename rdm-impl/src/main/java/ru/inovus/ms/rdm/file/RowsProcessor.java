package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.Result;

public interface RowsProcessor {

    Result append(Row row);

    Result process();

}

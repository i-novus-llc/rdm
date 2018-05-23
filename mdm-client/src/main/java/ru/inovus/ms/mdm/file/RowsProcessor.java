package ru.inovus.ms.mdm.file;

import ru.inovus.ms.mdm.model.Result;

public interface RowsProcessor {

    Result append(Row row);

    Result process();

}

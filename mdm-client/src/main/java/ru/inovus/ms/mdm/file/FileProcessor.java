package ru.inovus.ms.mdm.file;

import ru.inovus.ms.mdm.model.FileData;
import ru.inovus.ms.mdm.model.Result;

public interface FileProcessor {
    Result process(FileData file);

}

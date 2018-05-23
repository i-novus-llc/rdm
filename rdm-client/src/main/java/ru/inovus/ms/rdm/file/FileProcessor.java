package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.FileData;
import ru.inovus.ms.rdm.model.Result;

public interface FileProcessor {
    Result process(FileData file);

}

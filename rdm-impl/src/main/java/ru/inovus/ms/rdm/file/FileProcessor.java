package ru.inovus.ms.rdm.file;

import ru.inovus.ms.rdm.model.Result;

import java.io.InputStream;
import java.util.function.Supplier;

public interface FileProcessor {
    Result process(Supplier<InputStream> fileSource);
}
